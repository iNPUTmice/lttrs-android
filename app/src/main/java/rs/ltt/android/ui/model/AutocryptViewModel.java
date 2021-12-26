package rs.ltt.android.ui.model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import rs.ltt.android.entity.AccountName;
import rs.ltt.android.repository.AutocryptRepository;
import rs.ltt.android.repository.MainRepository;
import rs.ltt.autocrypt.client.header.EncryptionPreference;

public class AutocryptViewModel extends AndroidViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutocryptViewModel.class);

    private final long accountId;
    private final MainRepository mainRepository;
    private final AutocryptRepository autocryptRepository;
    private final LiveData<Boolean> autocryptEnabled;
    private final LiveData<EncryptionPreference> encryptionPreference;

    public AutocryptViewModel(@NonNull Application application, final long accountId) {
        super(application);
        this.accountId = accountId;
        this.mainRepository = new MainRepository(application);
        this.autocryptRepository = new AutocryptRepository(application, accountId);
        final LiveData<AccountName> accountNameLiveData =
                this.mainRepository.getAccountName(accountId);
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
                        LOGGER.debug(
                                "autocrypt encryption preference set to {}", encryptionPreference);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable throwable) {
                        LOGGER.error("could not set autocrypt encryption preference", throwable);
                    }
                },
                MoreExecutors.directExecutor());
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
                        LOGGER.error("could not set autocrypt enabled", throwable);
                    }
                },
                MoreExecutors.directExecutor());
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
