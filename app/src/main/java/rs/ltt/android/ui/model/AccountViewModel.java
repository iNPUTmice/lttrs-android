package rs.ltt.android.ui.model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import rs.ltt.android.entity.AccountName;
import rs.ltt.android.repository.MainRepository;


public class AccountViewModel extends AndroidViewModel {

    private final MainRepository mainRepository;
    private final long accountId;
    private final MutableLiveData<Boolean> enabled = new MutableLiveData<>(true);

    public AccountViewModel(@NonNull final Application application, final long accountId) {
        super(application);
        this.accountId = accountId;
        this.mainRepository = new MainRepository(application);
    }

    public LiveData<AccountName> getAccountName() {
        return this.mainRepository.getAccountName(this.accountId);
    }

    public LiveData<Boolean> isEnabled() {
        return this.enabled;
    }

    public void removeAccount() {
        this.enabled.postValue(false);
        final ListenableFuture<Void> future = this.mainRepository.removeAccountAsync(this.accountId);
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void unused) {
                //DO nothing. wait for finish
            }

            @Override
            public void onFailure(@NotNull Throwable throwable) {
                //display warning
                enabled.postValue(false);
            }
        }, MoreExecutors.directExecutor());
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
