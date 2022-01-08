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
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.MuaPool;
import rs.ltt.android.R;
import rs.ltt.android.util.Event;
import rs.ltt.autocrypt.jmap.AutocryptPlugin;
import rs.ltt.autocrypt.jmap.SetupMessage;
import rs.ltt.jmap.mua.Mua;
import rs.ltt.jmap.mua.service.exception.SetEmailException;

public class AutocryptExportViewModel extends AndroidViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutocryptExportViewModel.class);

    private final MutableLiveData<Event<String>> errorMessage = new MutableLiveData<>();

    private final long accountId;

    private final String passphrase = SetupMessage.generateSetupCode();

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public AutocryptExportViewModel(@NonNull Application application, final long accountId) {
        super(application);
        this.accountId = accountId;
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final Application application;
        private final long accountId;

        public Factory(@NonNull final Application application, @NonNull final long accountId) {
            this.application = application;
            this.accountId = accountId;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return Objects.requireNonNull(
                    modelClass.cast(new AutocryptExportViewModel(application, accountId)));
        }
    }

    public void storeSetupMessage() {
        final ListenableFuture<Mua> muaFuture = MuaPool.getInstance(getApplication(), accountId);
        this.loading.postValue(true);
        final ListenableFuture<String> setupMessageId =
                Futures.transformAsync(
                        muaFuture,
                        mua -> mua.getPlugin(AutocryptPlugin.class).storeSetupMessage(passphrase),
                        MoreExecutors.directExecutor());
        Futures.addCallback(
                setupMessageId,
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(final String messageId) {
                        // post redirect
                        loading.postValue(false);
                    }

                    @Override
                    public void onFailure(@NonNull final Throwable throwable) {
                        LOGGER.error("Could not store setup message", throwable);
                        loading.postValue(false);
                        postErrorMessage(throwable);
                    }
                },
                MoreExecutors.directExecutor());
    }

    private void postErrorMessage(final Throwable throwable) {
        final String message = throwable.getMessage();
        if (throwable instanceof SetEmailException) {
            errorMessage.postValue(
                    new Event<>(
                            getApplication().getString(R.string.could_not_store_setup_message)));
        } else if (message != null) {
            errorMessage.postValue(new Event<>(message));
        } else {
            errorMessage.postValue(new Event<>(throwable.getClass().getName()));
        }
    }

    public LiveData<Event<String>> getErrorMessage() {
        return this.errorMessage;
    }

    public LiveData<Boolean> isLoading() {
        return this.loading;
    }

    public String getPassphrase() {
        return this.passphrase;
    }
}
