package rs.ltt.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import rs.ltt.android.R;
import rs.ltt.android.databinding.FragmentAutocryptExportExplainedBinding;

public class AutocryptExportExplainedFragment extends AbstractAutocryptExportFragment {

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final FragmentAutocryptExportExplainedBinding binding =
                DataBindingUtil.inflate(
                        inflater, R.layout.fragment_autocrypt_export_explained, container, false);

        binding.next.setOnClickListener(this::onNextClicked);

        return binding.getRoot();
    }

    private void onNextClicked(View view) {
        getNavController()
                .navigate(AutocryptExportExplainedFragmentDirections.actionExplainedToSetupCode());
    }
}
