package rs.ltt.android.ui.model;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.R;
import rs.ltt.android.entity.AccountName;
import rs.ltt.android.repository.AutocryptRepository;
import rs.ltt.android.repository.MainRepository;
import rs.ltt.android.util.Event;
import rs.ltt.autocrypt.client.header.EncryptionPreference;

public class AutocryptViewModel extends AndroidViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutocryptViewModel.class);

    private final AutocryptRepository autocryptRepository;
    private final LiveData<Boolean> autocryptEnabled;
    private final LiveData<EncryptionPreference> encryptionPreference;

    private final MutableLiveData<Event<String>> errorMessage = new MutableLiveData<>();

    public AutocryptViewModel(@NonNull Application application, final long accountId) {
        super(application);
        final MainRepository mainRepository = new MainRepository(application);
        this.autocryptRepository = new AutocryptRepository(application, accountId);
        final ListenableFuture<Void> init = this.autocryptRepository.ensureEverythingIsSetup();
        Futures.addCallback(
                init,
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(Void unused) {
                        LOGGER.debug("autocrypt client is setup correctly");
                    }

                    @Override
                    public void onFailure(@NonNull Throwable throwable) {
                        postErrorMessage(throwable);
                    }
                },
                MoreExecutors.directExecutor());
        final LiveData<AccountName> accountNameLiveData = mainRepository.getAccountName(accountId);
        this.autocryptEnabled =
                Transformations.switchMap(
                        accountNameLiveData,
                        accountName ->
                                this.autocryptRepository.isAutocryptEnabled(accountName.getName()));
        this.encryptionPreference =
                Transformations.switchMap(
                        accountNameLiveData,
                        accountName ->
                                this.autocryptRepository.getEncryptionPreference(
                                        accountName.getName()));
    }

    public LiveData<Boolean> isAutocryptEnabled() {
        return this.autocryptEnabled;
    }

    public LiveData<EncryptionPreference> getEncryptionPreference() {
        return this.encryptionPreference;
    }

    public void setEncryptionPreference(final EncryptionPreference encryptionPreference) {
        final EncryptionPreference currentValue = this.encryptionPreference.getValue();
        if (currentValue != null && currentValue == encryptionPreference) {
            return;
        }
        final ListenableFuture<Void> future =
                this.autocryptRepository.setEncryptionPreference(encryptionPreference);
        Futures.addCallback(
                future,
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(Void unused) {
                        LOGGER.info(
                                "autocrypt encryption preference set to {}", encryptionPreference);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable throwable) {
                        postErrorMessage(throwable);
                    }
                },
                MoreExecutors.directExecutor());
    }

    private void postErrorMessage(final Throwable throwable) {
        final String message = throwable.getMessage();
        if (throwable instanceof NoSuchAlgorithmException) {
            postErrorMessage(R.string.encryption_provider_can_not_create_secret_key);
        } else if (Strings.isNullOrEmpty(message)) {
            postErrorMessage(R.string.could_not_configure_autocrypt);
        } else {
            postErrorMessage(message);
        }
    }

    private void postErrorMessage(final @StringRes int stringRes) {
        postErrorMessage(getApplication().getString(stringRes));
    }

    private void postErrorMessage(final String message) {
        this.errorMessage.postValue(new Event<>(message));
    }

    public LiveData<Event<String>> getErrorMessage() {
        return this.errorMessage;
    }

    public void setAutocryptEnabled(final boolean enabled) {
        final Boolean currentValue = this.autocryptEnabled.getValue();
        if (currentValue != null && currentValue == enabled) {
            return;
        }
        final ListenableFuture<Void> future = this.autocryptRepository.setEnabled(enabled);
        Futures.addCallback(
                future,
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(Void unused) {
                        LOGGER.debug("autocrypt enabled set to {}", enabled);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable throwable) {
                        postErrorMessage(throwable);
                    }
                },
                MoreExecutors.directExecutor());
    }

    public long getAccountId() {
        return this.autocryptRepository.getAccountId();
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final Application application;
        private final long accountId;

        public Factory(@NonNull final Application application, final long accountId) {
            this.application = application;
            this.accountId = accountId;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return Objects.requireNonNull(
                    modelClass.cast(new AutocryptViewModel(application, accountId)));
        }
    }
}
