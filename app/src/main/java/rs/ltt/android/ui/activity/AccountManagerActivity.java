package rs.ltt.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;

import java.util.Objects;

import rs.ltt.android.R;
import rs.ltt.android.databinding.ActivityAccountManagerBinding;
import rs.ltt.android.util.NavControllers;

public class AccountManagerActivity extends AppCompatActivity {

    private ActivityAccountManagerBinding binding;

    public static void launch(final AppCompatActivity activity) {
        final Intent intent = new Intent(activity, AccountManagerActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = DataBindingUtil.setContentView(this, R.layout.activity_account_manager);

        final NavController navController = getNavController();
        navController.addOnDestinationChangedListener(this::onDestinationChanged);

        configureActionBar();
    }

    private void onDestinationChanged(NavController navController, NavDestination navDestination, Bundle bundle) {
        setTitle(navDestination.getLabel());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                final NavController navController = getNavController();
                if (navController.popBackStack()) {
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);

    }

    private void configureActionBar() {
        this.setSupportActionBar(this.binding.toolbar);
        final ActionBar actionBar = Objects.requireNonNull(getSupportActionBar());
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    public NavController getNavController() {
        return NavControllers.findNavController(this, R.id.nav_host_fragment);
    }
}
