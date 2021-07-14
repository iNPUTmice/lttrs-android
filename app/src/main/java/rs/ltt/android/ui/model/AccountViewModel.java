package rs.ltt.android.ui.model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.Objects;

import rs.ltt.android.entity.AccountName;
import rs.ltt.android.repository.MainRepository;


public class AccountViewModel extends AndroidViewModel {

    private final MainRepository mainRepository;
    private final long accountId;

    public AccountViewModel(@NonNull final Application application, final long accountId) {
        super(application);
        this.accountId = accountId;
        this.mainRepository = new MainRepository(application);
    }

    public LiveData<AccountName> getAccountName() {
        return this.mainRepository.getAccountName(this.accountId);
    }

    public void removeAccount() {
        this.mainRepository.removeAccountAsync(this.accountId);
    }

    public long getAccountId() {
        return this.accountId;
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
            return Objects.requireNonNull(modelClass.cast(new AccountViewModel(application, accountId)));
        }
    }
}
