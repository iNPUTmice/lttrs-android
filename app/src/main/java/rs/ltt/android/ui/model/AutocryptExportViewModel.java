package rs.ltt.android.ui.model;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import java.util.Objects;
import rs.ltt.android.util.Event;
import rs.ltt.autocrypt.jmap.SetupMessage;

public class AutocryptExportViewModel extends AndroidViewModel {

    private final MutableLiveData<Event<String>> errorMessage = new MutableLiveData<>();

    private final long accountId;

    private String passphrase = SetupMessage.generateSetupCode();

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

    public LiveData<Event<String>> getErrorMessage() {
        return this.errorMessage;
    }

    public String getPassphrase() {
        return this.passphrase;
    }
}
