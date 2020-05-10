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
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;

import androidx.annotation.NonNull;
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

import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

import okhttp3.HttpUrl;
import rs.ltt.android.R;
import rs.ltt.android.repository.MainRepository;
import rs.ltt.android.util.Event;
import rs.ltt.jmap.client.JmapClient;
import rs.ltt.jmap.client.api.EndpointNotFoundException;
import rs.ltt.jmap.client.api.InvalidSessionResourceException;
import rs.ltt.jmap.client.api.UnauthorizedException;
import rs.ltt.jmap.client.session.Session;
import rs.ltt.jmap.common.entity.Account;
import rs.ltt.jmap.common.entity.capability.MailAccountCapability;
import rs.ltt.jmap.mua.util.EmailAddressUtil;

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
    private final MutableLiveData<Event<String>> warningMessage = new MutableLiveData<>();

    private final MainRepository mainRepository;

    public SetupViewModel(@NonNull Application application) {
        super(application);
        this.mainRepository = new MainRepository(application);
        Transformations.distinctUntilChanged(emailAddress).observeForever(s -> emailAddressError.postValue(null));
        Transformations.distinctUntilChanged(sessionResource).observeForever(s -> sessionResourceError.postValue(null));
        Transformations.distinctUntilChanged(password).observeForever(s -> passwordError.postValue(null));
    }

    private static boolean isEndpointProblem(Throwable t) {
        return t instanceof InvalidSessionResourceException
                || t instanceof EndpointNotFoundException
                || t instanceof ConnectException
                || t instanceof SocketTimeoutException
                || t instanceof SSLHandshakeException
                || t instanceof SSLPeerUnverifiedException;
    }

    public LiveData<Boolean> isLoading() {
        return this.loading;
    }

    public LiveData<String> getEmailAddressError() {
        return Transformations.distinctUntilChanged(emailAddressError);
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
            Futures.addCallback(getSession(), new FutureCallback<Session>() {
                @Override
                public void onSuccess(@NullableDecl Session session) {
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
                            emailAddressError.postValue(getApplication().getString(R.string.no_network_connection));
                        }
                    } else if (isEndpointProblem(cause)) {
                        sessionResourceError.postValue(null);
                        redirection.postValue(new Event<>(Target.ENTER_URL));
                    } else {
                        reportUnableToFetchSession(cause);
                    }
                }
            }, MoreExecutors.directExecutor());
        } else {
            if (emailAddress.isEmpty()) {
                emailAddressError.postValue(
                        getApplication().getString(R.string.enter_an_email_address)
                );
            } else {
                emailAddressError.postValue(
                        getApplication().getString(R.string.enter_a_valid_email_address)
                );
            }
        }
        return true;
    }

    public boolean enterPassword() {
        final String password = Strings.nullToEmpty(this.password.getValue());
        if (password.isEmpty()) {
            this.passwordError.postValue(
                    getApplication().getString(R.string.enter_a_password)
            );
        } else {
            this.loading.postValue(true);
            this.passwordError.postValue(null);
            Futures.addCallback(getSession(), new FutureCallback<Session>() {
                @Override
                public void onSuccess(@NullableDecl Session session) {
                    Preconditions.checkNotNull(session);
                    processAccounts(session);
                }

                @Override
                public void onFailure(@NonNull Throwable cause) {
                    loading.postValue(false);
                    if (cause instanceof UnauthorizedException) {
                        passwordError.postValue(getApplication().getString(R.string.wrong_password));
                    } else if (cause instanceof UnknownHostException) {
                        if (isNetworkAvailable()) {
                            sessionResourceError.postValue(null);
                            redirection.postValue(new Event<>(Target.ENTER_URL));
                        } else {
                            passwordError.postValue(getApplication().getString(R.string.no_network_connection));
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
            }, MoreExecutors.directExecutor());
        }
        return true;
    }

    public boolean enterSessionResource() {
        try {
            final HttpUrl httpUrl = HttpUrl.get(Strings.nullToEmpty(sessionResource.getValue()));
            LOGGER.debug("User entered connection url {}", httpUrl.toString());
            if (httpUrl.scheme().equals("http")) {
                this.sessionResourceError.postValue(getApplication().getString(R.string.enter_a_secure_url));
                return true;
            }
            this.loading.postValue(true);
            this.sessionResource.postValue(httpUrl.toString());
            this.sessionResourceError.postValue(null);
            Futures.addCallback(getSession(), new FutureCallback<Session>() {
                @Override
                public void onSuccess(@NullableDecl Session session) {
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
                            sessionResourceError.postValue(getApplication().getString(R.string.unknown_host, httpUrl.host()));
                        } else {
                            sessionResourceError.postValue(getApplication().getString(R.string.no_network_connection));
                        }
                    } else {
                        reportUnableToFetchSession(cause);
                    }
                }
            }, MoreExecutors.directExecutor());
        } catch (IllegalArgumentException e) {
            this.sessionResourceError.postValue(getApplication().getString(R.string.enter_a_valid_url));
        }
        return true;
    }

    private ListenableFuture<Session> getSession() {
        final JmapClient jmapClient = new JmapClient(
                Strings.nullToEmpty(emailAddress.getValue()),
                Strings.nullToEmpty(password.getValue()),
                getHttpSessionResource()
        );
        return jmapClient.getSession();
    }

    private void processAccounts(final Session session) {
        final Map<String, Account> accounts = session.getAccounts(MailAccountCapability.class);
        LOGGER.info("found {} accounts with mail capability", accounts.size());
        if (accounts.size() == 1) {
            final ListenableFuture<Void> insertFuture = mainRepository.insertAccountsRefreshMailboxes(
                    Strings.nullToEmpty(emailAddress.getValue()),
                    Strings.nullToEmpty(password.getValue()),
                    getHttpSessionResource(),
                    session.getPrimaryAccount(MailAccountCapability.class),
                    accounts
            );
            Futures.addCallback(insertFuture, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@NullableDecl Void result) {
                    redirection.postValue(new Event<>(Target.LTTRS));
                }

                @Override
                public void onFailure(@NonNull Throwable cause) {
                    loading.postValue(false);
                    warningMessage.postValue(new Event<>(
                            getApplication().getString(R.string.could_not_store_account_credentials)
                    ));

                }
            }, MoreExecutors.directExecutor());
        } else {
            loading.postValue(false);
            redirection.postValue(new Event<>(Target.SELECT_ACCOUNTS));
            //store accounts in view model
        }
    }

    private void reportUnableToFetchSession(final Throwable throwable) {
        if (throwable instanceof InterruptedException) {
            return;
        }
        LOGGER.error("Unexpected problem fetching session object", throwable);
        final String message = throwable.getMessage();
        this.warningMessage.postValue(new Event<>(
                getApplication().getString(
                        R.string.unable_to_fetch_session,
                        (message == null ? throwable.getClass().getSimpleName() : message)
                )
        ));
    }

    private boolean isNetworkAvailable() {
        final ConnectivityManager cm = getApplication().getSystemService(ConnectivityManager.class);
        final Network activeNetwork = cm == null ? null : cm.getActiveNetwork();
        return activeNetwork != null;
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


    public enum Target {
        ENTER_PASSWORD,
        ENTER_URL,
        SELECT_ACCOUNTS,
        LTTRS
    }
}
