package rs.ltt.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import java.util.Objects;
import rs.ltt.android.R;
import rs.ltt.android.databinding.ActivityAccountManagerBinding;
import rs.ltt.android.util.NavControllers;

public class AccountManagerActivity extends AppCompatActivity {

    public static void launch(final AppCompatActivity activity) {
        final Intent intent = new Intent(activity, AccountManagerActivity.class);
        activity.startActivity(intent);
    }

    public static void relaunch(final FragmentActivity activity) {
        final Intent intent = new Intent(activity, AccountManagerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActivityAccountManagerBinding binding =
                DataBindingUtil.setContentView(this, R.layout.activity_account_manager);

        this.setSupportActionBar(binding.toolbar);

        final NavController navController = getNavController();
        navController.addOnDestinationChangedListener(this::onDestinationChanged);
    }

    private void onDestinationChanged(
            final NavController navController,
            final NavDestination navDestination,
            final Bundle bundle) {
        setTitle(navDestination.getLabel());
        final boolean root = isTaskRoot() && navDestination.getId() == R.id.account_list;
        configureActionBar(!root);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        if (id == android.R.id.home) {
            final NavController navController = getNavController();
            if (navController.popBackStack()) {
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void configureActionBar(final boolean upEnabled) {
        final ActionBar actionBar = Objects.requireNonNull(getSupportActionBar());
        actionBar.setHomeButtonEnabled(upEnabled);
        actionBar.setDisplayHomeAsUpEnabled(upEnabled);
    }

    public NavController getNavController() {
        return NavControllers.findNavController(this, R.id.nav_host_fragment);
    }
}
