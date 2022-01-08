package rs.ltt.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import java.util.Objects;
import rs.ltt.android.R;
import rs.ltt.android.databinding.ActivityAutocryptExportBinding;
import rs.ltt.android.ui.MaterialAlertDialogs;
import rs.ltt.android.ui.model.AutocryptExportViewModel;
import rs.ltt.android.util.Event;
import rs.ltt.android.util.NavControllers;

public class AutocryptExportActivity extends AppCompatActivity {

    public static final String EXTRA_ACCOUNT_ID = "account";

    public static void launch(final FragmentActivity activity, final long accountId) {
        final Intent intent = new Intent(activity, AutocryptExportActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.putExtra(AutocryptExportActivity.EXTRA_ACCOUNT_ID, accountId);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setWindowFlags(
                WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        final ActivityAutocryptExportBinding binding =
                DataBindingUtil.setContentView(this, R.layout.activity_autocrypt_export);

        final Intent intent = getIntent();
        final long accountId;
        if (intent != null && intent.hasExtra(EXTRA_ACCOUNT_ID)) {
            accountId = intent.getLongExtra(EXTRA_ACCOUNT_ID, -1);
        } else {
            throw new IllegalStateException(
                    AutocryptExportActivity.class.getSimpleName()
                            + " can not be started without account parameter");
        }

        final ViewModelProvider viewModelProvider =
                new ViewModelProvider(
                        getViewModelStore(),
                        new AutocryptExportViewModel.Factory(getApplication(), accountId));
        final AutocryptExportViewModel autocryptExportViewModel =
                viewModelProvider.get(AutocryptExportViewModel.class);

        autocryptExportViewModel.getErrorMessage().observe(this, this::onErrorMessage);

        this.setSupportActionBar(binding.toolbar);
        configureActionBar();
    }

    private void setWindowFlags(final int flags, final int mask) {
        getWindow().setFlags(flags, mask);
    }

    private void onErrorMessage(final Event<String> event) {
        MaterialAlertDialogs.error(this, event);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void configureActionBar() {
        final ActionBar actionBar = Objects.requireNonNull(getSupportActionBar());
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_close_24);
    }

    public NavController getNavController() {
        return NavControllers.findNavController(this, R.id.nav_host_fragment);
    }
}
