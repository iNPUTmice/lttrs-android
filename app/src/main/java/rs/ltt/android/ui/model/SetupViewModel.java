/*
 * Copyright 2019 Daniel Gultsch
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rs.ltt.android.ui.model;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import okhttp3.HttpUrl;
import org.pgpainless.exception.MissingDecryptionMethodException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.BuildConfig;
import rs.ltt.android.LttrsApplication;
import rs.ltt.android.R;
import rs.ltt.android.entity.AutocryptSetupMessage;
import rs.ltt.android.repository.MainRepository;
import rs.ltt.android.util.Event;
import rs.ltt.autocrypt.client.SetupCode;
import rs.ltt.jmap.client.JmapClient;
import rs.ltt.jmap.client.api.EndpointNotFoundException;
import rs.ltt.jmap.client.api.InvalidSessionResourceException;
import rs.ltt.jmap.client.api.UnauthorizedException;
import rs.ltt.jmap.client.session.Session;
import rs.ltt.jmap.common.entity.Account;
import rs.ltt.jmap.common.entity.capability.MailAccountCapability;
import rs.ltt.jmap.mua.util.EmailAddressUtil;

// TODO hold on to most recent ListenableFuture and cancel on back press
public class SetupViewModel extends AndroidViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetupViewModel.class);

    private final MutableLiveData<String> emailAddress = new MutableLiveData<>();
    private final MutableLiveData<String> emailAddressError = new MutableLiveData<>();
    private final MutableLiveData<String> password = new MutableLiveData<>();
    private final MutableLiveData<String> passwordError = new MutableLiveData<>();
    private final MutableLiveData<String> sessionResource = new MutableLiveData<>();
    private final MutableLiveData<String> sessionResourceError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Event<Target>> redirection = new MutableLiveData<>();
    private Long primaryAccountId = null;
    private final MutableLiveData<Event<String>> warningMessage = new MutableLiveData<>();
    private final MainRepository mainRepository;
    private ListenableFuture<?> networkFuture = null;
    private final Queue<AutocryptSetupMessage> setupMessages = new LinkedList<>();

    public SetupViewModel(@NonNull Application application) {
        super(application);
        this.mainRepository = new MainRepository(application);
        Transformations.distinctUntilChanged(emailAddress)
                .observeForever(s -> emailAddressError.postValue(null));
        Transformations.distinctUntilChanged(sessionResource)
                .observeForever(s -> sessionResourceError.postValue(null));
        Transformations.distinctUntilChanged(password)
                .observeForever(s -> passwordError.postValue(null));
    }

    private static boolean isEndpointProblem(Throwable t) {
        return t instanceof InvalidSessionResourceException
                || t instanceof EndpointNotFoundException
                || t instanceof ConnectException
                || t instanceof SocketTimeoutException
                || t instanceof SSLHandshakeException
                || t instanceof SSLPeerUnverifiedException;
    }

    private static boolean secure(final HttpUrl url) {
        return url.scheme().equals("https")
                || (BuildConfig.DEBUG && url.host().equals("localhost"));
    }

    private static boolean interruptedOrCancelled(final Throwable t) {
        return t instanceof InterruptedException || t instanceof CancellationException;
    }

    public LiveData<Boolean> isLoading() {
        return this.loading;
    }

    public LiveData<String> getEmailAddressError() {
        return Transformations.distinctUntilChanged(emailAddressError);
    }

    public AutocryptSetupMessage peekSetupMessage() {
        return this.setupMessages.peek();
    }

    public MutableLiveData<String> getEmailAddress() {
        return emailAddress;
    }

    public MutableLiveData<String> getPassword() {
        return password;
    }

    public LiveData<String> getPasswordError() {
        return Transformations.distinctUntilChanged(this.passwordError);
    }

    public MutableLiveData<String> getSessionResource() {
        return sessionResource;
    }

    public LiveData<String> getSessionResourceError() {
        return Transformations.distinctUntilChanged(sessionResourceError);
    }

    public LiveData<Event<Target>> getRedirection() {
        return this.redirection;
    }

    public LiveData<Event<String>> getWarningMessage() {
        return this.warningMessage;
    }

    public boolean enterEmailAddress() {
        this.password.setValue(null);
        this.sessionResource.setValue(null);
        final String emailAddress = Strings.nullToEmpty(this.emailAddress.getValue()).trim();
        if (EmailAddressUtil.isValid(emailAddress)) {
            this.loading.postValue(true);
            this.emailAddressError.postValue(null);
            this.emailAddress.postValue(emailAddress);
            Futures.addCallback(
                    getSession(),
                    new FutureCallback<>() {
                        @Override
                        public void onSuccess(@Nullable Session session) {
                            Preconditions.checkNotNull(session);
                            processAccounts(session);
                        }

                        @Override
                        public void onFailure(@NonNull Throwable cause) {
                            loading.postValue(false);
                            if (cause instanceof UnauthorizedException) {
                                passwordError.postValue(null);
                                redirection.postValue(new Event<>(Target.ENTER_PASSWORD));
                            } else if (cause instanceof UnknownHostException) {
                                if (isNetworkAvailable()) {
                                    sessionResourceError.postValue(null);
                                    redirection.postValue(new Event<>(Target.ENTER_URL));
                                } else {
                                    emailAddressError.postValue(
                                            getApplication()
                                                    .getString(R.string.no_network_connection));
                                }
                            } else if (isEndpointProblem(cause)) {
                                sessionResourceError.postValue(null);
                                redirection.postValue(new Event<>(Target.ENTER_URL));
                            } else {
                                reportUnableToFetchSession(cause);
                            }
                        }
                    },
                    MoreExecutors.directExecutor());
        } else {
            if (emailAddress.isEmpty()) {
                emailAddressError.postValue(
                        getApplication().getString(R.string.enter_an_email_address));
            } else {
                emailAddressError.postValue(
                        getApplication().getString(R.string.enter_a_valid_email_address));
            }
        }
        return true;
    }

    public boolean enterPassword() {
        final String password = Strings.nullToEmpty(this.password.getValue());
        if (password.isEmpty()) {
            this.passwordError.postValue(getApplication().getString(R.string.enter_a_password));
        } else {
            this.loading.postValue(true);
            this.passwordError.postValue(null);
            Futures.addCallback(
                    getSession(),
                    new FutureCallback<>() {
                        @Override
                        public void onSuccess(@Nullable Session session) {
                            Preconditions.checkNotNull(session);
                            processAccounts(session);
                        }

                        @Override
                        public void onFailure(@NonNull Throwable cause) {
                            loading.postValue(false);
                            if (cause instanceof UnauthorizedException) {
                                passwordError.postValue(
                                        getApplication().getString(R.string.wrong_password));
                            } else if (cause instanceof UnknownHostException) {
                                if (isNetworkAvailable()) {
                                    sessionResourceError.postValue(null);
                                    redirection.postValue(new Event<>(Target.ENTER_URL));
                                } else {
                                    passwordError.postValue(
                                            getApplication()
                                                    .getString(R.string.no_network_connection));
                                }
                            } else if (isEndpointProblem(cause)) {
                                if (Strings.emptyToNull(sessionResource.getValue()) != null) {
                                    sessionResourceError.postValue(causeToString(cause));
                                } else {
                                    sessionResourceError.postValue(null);
                                }
                                redirection.postValue(new Event<>(Target.ENTER_URL));
                            } else {
                                reportUnableToFetchSession(cause);
                            }
                        }
                    },
                    MoreExecutors.directExecutor());
        }
        return true;
    }

    public boolean enterSessionResource() {
        final HttpUrl httpUrl;
        try {
            httpUrl = HttpUrl.get(Strings.nullToEmpty(sessionResource.getValue()));
        } catch (final IllegalArgumentException e) {
            this.sessionResourceError.postValue(
                    getApplication().getString(R.string.enter_a_valid_url));
            return true;
        }
        LOGGER.debug("User entered connection url {}", httpUrl.toString());
        if (!secure(httpUrl)) {
            this.sessionResourceError.postValue(
                    getApplication().getString(R.string.enter_a_secure_url));
            return true;
        }
        this.loading.postValue(true);
        this.sessionResource.postValue(httpUrl.toString());
        this.sessionResourceError.postValue(null);
        Futures.addCallback(
                getSession(),
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable Session session) {
                        Preconditions.checkNotNull(session);
                        processAccounts(session);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable cause) {
                        loading.postValue(false);
                        if (cause instanceof UnauthorizedException) {
                            passwordError.postValue(null);
                            redirection.postValue(new Event<>(Target.ENTER_PASSWORD));
                        } else if (isEndpointProblem(cause)) {
                            sessionResourceError.postValue(causeToString(cause));
                        } else if (cause instanceof UnknownHostException) {
                            if (isNetworkAvailable()) {
                                sessionResourceError.postValue(
                                        getApplication()
                                                .getString(R.string.unknown_host, httpUrl.host()));
                            } else {
                                sessionResourceError.postValue(
                                        getApplication().getString(R.string.no_network_connection));
                            }
                        } else {
                            reportUnableToFetchSession(cause);
                        }
                    }
                },
                MoreExecutors.directExecutor());
        return true;
    }

    public boolean enterSetupCode(final String passphrase) {
        if (SetupCode.isValid(passphrase)) {
            final AutocryptSetupMessage autocryptSetupMessage = this.peekSetupMessage();
            if (autocryptSetupMessage == null) {
                throw new IllegalStateException("Not pending Autocrypt Setup Message found");
            }
            this.loading.postValue(true);
            final ListenableFuture<Void> secretKeyImport =
                    mainRepository.importAutocryptSetupMessage(autocryptSetupMessage, passphrase);
            Futures.addCallback(
                    secretKeyImport,
                    new FutureCallback<>() {
                        @Override
                        public void onSuccess(final Void unused) {
                            loading.postValue(false);
                            nextPrivateKeyImport();
                        }

                        @Override
                        public void onFailure(@NonNull Throwable throwable) {
                            loading.postValue(false);
                            LOGGER.error("Unable to import secret key", throwable);
                            final String message = throwable.getMessage();
                            if (throwable instanceof MissingDecryptionMethodException) {
                                showWarningMessage(R.string.wrong_setup_code);
                            } else if (Strings.isNullOrEmpty(message)) {
                                showWarningMessage(throwable.getClass().getName());
                            } else {
                                showWarningMessage(message);
                            }
                        }
                    },
                    MoreExecutors.directExecutor());
        } else {
            showWarningMessage(R.string.enter_your_setup_code);
        }
        return true;
    }

    public void nextPrivateKeyImport() {
        final AutocryptSetupMessage setupMessage = this.setupMessages.poll();
        if (setupMessage == null || this.setupMessages.isEmpty()) {
            this.redirection.postValue(new Event<>(Target.DONE));
        } else {
            // go to next private key import
            this.redirection.postValue(new Event<>(Target.IMPORT_PRIVATE_KEY));
        }
    }

    public boolean cancel() {
        final boolean cancelledModelFuture = cancelNetworkFuture();
        final boolean cancelledRepositoryFuture = this.mainRepository.cancelNetworkFuture();
        return cancelledRepositoryFuture || cancelledModelFuture;
    }

    private boolean cancelNetworkFuture() {
        final ListenableFuture<?> currentNetworkFuture = this.networkFuture;
        if (currentNetworkFuture == null || currentNetworkFuture.isDone()) {
            return false;
        }
        return currentNetworkFuture.cancel(true);
    }

    private ListenableFuture<Session> getSession() {
        final JmapClient jmapClient =
                new JmapClient(
                        Strings.nullToEmpty(emailAddress.getValue()),
                        Strings.nullToEmpty(password.getValue()),
                        getHttpSessionResource());
        final ListenableFuture<Session> sessionFuture = jmapClient.getSession();
        this.networkFuture = sessionFuture;
        return sessionFuture;
    }

    private void processAccounts(final Session session) {
        final Map<String, Account> accounts = session.getAccounts(MailAccountCapability.class);
        LOGGER.info("found {} accounts with mail capability", accounts.size());
        if (accounts.size() == 1) {
            final ListenableFuture<MainRepository.InsertOperation> insertFuture =
                    mainRepository.insertAccountDiscoverSetupMessage(
                            Strings.nullToEmpty(emailAddress.getValue()),
                            Strings.nullToEmpty(password.getValue()),
                            getHttpSessionResource(),
                            session.getPrimaryAccount(MailAccountCapability.class),
                            accounts);
            Futures.addCallback(
                    insertFuture,
                    new FutureCallback<>() {
                        @Override
                        public void onSuccess(MainRepository.InsertOperation operation) {
                            processInsertOperation(operation);
                        }

                        @Override
                        public void onFailure(@NonNull Throwable cause) {
                            LOGGER.error("Could not store account", cause);
                            loading.postValue(false);
                            showWarningMessage(R.string.could_not_store_account_credentials);
                        }
                    },
                    MoreExecutors.directExecutor());
        } else {
            loading.postValue(false);
            redirection.postValue(new Event<>(Target.SELECT_ACCOUNTS));
            // store accounts in view model
        }
    }

    private void showWarningMessage(final @StringRes int res) {
        showWarningMessage(getApplication().getString(res));
    }

    private void showWarningMessage(final String message) {
        this.warningMessage.postValue(new Event<>(message));
    }

    private void processInsertOperation(final MainRepository.InsertOperation operation) {
        LOGGER.info("processing insert operation");
        this.primaryAccountId = operation.getId();
        if (operation.getSetupMessages().isEmpty()) {
            LttrsApplication.get(getApplication()).invalidateMostRecentlySelectedAccountId();
            mainRepository.setSelectedAccount(operation.getId());
            redirection.postValue(new Event<>(Target.DONE));
        } else {
            this.setupMessages.addAll(operation.getSetupMessages());
            this.loading.postValue(false);
            this.redirection.postValue(new Event<>(Target.IMPORT_PRIVATE_KEY));
        }
    }

    private void reportUnableToFetchSession(final Throwable throwable) {
        if (interruptedOrCancelled(throwable)) {
            return;
        }
        LOGGER.error("Unexpected problem fetching session object", throwable);
        final String message = throwable.getMessage();
        this.warningMessage.postValue(
                new Event<>(
                        getApplication()
                                .getString(
                                        R.string.unable_to_fetch_session,
                                        (message == null
                                                ? throwable.getClass().getSimpleName()
                                                : message))));
    }

    private boolean isNetworkAvailable() {
        final ConnectivityManager cm = getApplication().getSystemService(ConnectivityManager.class);
        final Network activeNetwork = cm == null ? null : cm.getActiveNetwork();
        final NetworkCapabilities capabilities =
                activeNetwork == null ? null : cm.getNetworkCapabilities(activeNetwork);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private HttpUrl getHttpSessionResource() {
        final String sessionResource = Strings.emptyToNull(this.sessionResource.getValue());
        if (sessionResource == null) {
            return null;
        } else {
            return HttpUrl.get(sessionResource);
        }
    }

    private String causeToString(Throwable t) {
        final Context c = getApplication();
        if (t instanceof InvalidSessionResourceException) {
            return c.getString(R.string.invalid_session_resource);
        }
        if (t instanceof EndpointNotFoundException) {
            return c.getString(R.string.endpoint_not_found);
        }
        if (t instanceof ConnectException) {
            return c.getString(R.string.unable_to_connect);
        }
        if (t instanceof SocketTimeoutException) {
            return c.getString(R.string.timeout_reached);
        }
        if (t instanceof SSLHandshakeException) {
            return c.getString(R.string.unable_to_establish_secure_connection);
        }
        if (t instanceof SSLPeerUnverifiedException) {
            return c.getString(R.string.unable_to_verify_service_identity);
        }
        throw new IllegalArgumentException();
    }

    public long getPrimaryAccountId() {
        final Long accountId = this.primaryAccountId;
        if (accountId == null) {
            throw new IllegalStateException(
                    "Trying to access accountId before Target.DONE event occured");
        }
        return accountId;
    }

    public enum Target {
        ENTER_PASSWORD,
        ENTER_URL,
        SELECT_ACCOUNTS,
        DONE,
        IMPORT_PRIVATE_KEY;
    }
}
