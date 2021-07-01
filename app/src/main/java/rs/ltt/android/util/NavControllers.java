package rs.ltt.android.util;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

public final class NavControllers {

    private NavControllers() {

    }

    public static NavController findNavController(final AppCompatActivity activity, @IdRes int fragmentId) {
        final FragmentManager fragmentManager = activity.getSupportFragmentManager();
        final Fragment fragment = fragmentManager.findFragmentById(fragmentId);
        if (fragment instanceof NavHostFragment) {
            return ((NavHostFragment) fragment).getNavController();
        }
        throw new IllegalStateException("Fragment was not of type NavHostFragment");
    }
}
