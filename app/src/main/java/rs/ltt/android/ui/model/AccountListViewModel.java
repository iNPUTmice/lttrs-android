package rs.ltt.android.ui.model;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import rs.ltt.android.entity.AccountName;
import rs.ltt.android.repository.MainRepository;

public class AccountListViewModel extends AndroidViewModel {

    private final MainRepository mainRepository;

    public AccountListViewModel(@NonNull Application application) {
        super(application);
        this.mainRepository = new MainRepository(application);
    }

    public LiveData<List<AccountName>> getAccounts() {
        return this.mainRepository.getAccountNames();
    }
}
