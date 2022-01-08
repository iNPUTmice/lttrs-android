package rs.ltt.android.ui.fragment;

import android.app.Activity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import rs.ltt.android.ui.activity.AutocryptExportActivity;
import rs.ltt.android.ui.model.AutocryptExportViewModel;

public abstract class AbstractAutocryptExportFragment extends Fragment {

    AutocryptExportViewModel getAutocryptExportViewModel() {
        final ViewModelProvider viewModelProvider =
                new ViewModelProvider(requireActivity(), getDefaultViewModelProviderFactory());
        return viewModelProvider.get(AutocryptExportViewModel.class);
    }

    AutocryptExportActivity requireAutocryptExportActivity() {
        final Activity activity = getActivity();
        if (activity instanceof AutocryptExportActivity) {
            return (AutocryptExportActivity) activity;
        }
        throw new IllegalStateException("Fragment is not attached to AutocryptExportActivity");
    }

    NavController getNavController() {
        return requireAutocryptExportActivity().getNavController();
    }
}
