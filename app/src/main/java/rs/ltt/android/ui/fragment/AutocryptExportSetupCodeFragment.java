package rs.ltt.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import rs.ltt.android.R;
import rs.ltt.android.databinding.FragmentAutocryptExportSetupCodeBinding;
import rs.ltt.android.ui.model.AutocryptExportViewModel;
import rs.ltt.android.util.Event;

public class AutocryptExportSetupCodeFragment extends AbstractAutocryptExportFragment {

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final FragmentAutocryptExportSetupCodeBinding binding =
                DataBindingUtil.inflate(
                        inflater, R.layout.fragment_autocrypt_export_setup_code, container, false);
        final AutocryptExportViewModel viewModel = getAutocryptExportViewModel();
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setAutocryptViewModel(viewModel);
        getAutocryptExportViewModel()
                .getSetupMessageCreated()
                .observe(getViewLifecycleOwner(), this::onSetupMessageCreated);
        return binding.getRoot();
    }

    private void onSetupMessageCreated(final Event<Void> event) {
        if (event.isConsumable()) {
            event.consume();
            getNavController()
                    .navigate(AutocryptExportSetupCodeFragmentDirections.actionCodeToDone());
        }
    }
}
