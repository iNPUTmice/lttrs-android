package rs.ltt.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = DataBindingUtil.setContentView(this, R.layout.activity_account_manager);

        final NavController navController = NavControllers.findNavController(this, R.id.nav_host_fragment);
        navController.addOnDestinationChangedListener(this::onDestinationChanged);

        configureActionBar();
    }

    private void onDestinationChanged(NavController navController, NavDestination navDestination, Bundle bundle) {
        setTitle(navDestination.getLabel());
    }

    private void configureActionBar() {
        this.setSupportActionBar(this.binding.toolbar);
        final ActionBar actionBar = Objects.requireNonNull(getSupportActionBar());
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

}
