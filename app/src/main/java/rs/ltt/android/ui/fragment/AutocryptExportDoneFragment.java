package rs.ltt.android.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import rs.ltt.android.R;
import rs.ltt.android.databinding.FragmentAutocryptExportDoneBinding;
import rs.ltt.android.ui.activity.LttrsActivity;
import rs.ltt.android.ui.model.AutocryptExportViewModel;

public class AutocryptExportDoneFragment extends AbstractAutocryptExportFragment {

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final FragmentAutocryptExportDoneBinding binding =
                DataBindingUtil.inflate(
                        inflater, R.layout.fragment_autocrypt_export_done, container, false);
        binding.setModel(getAutocryptExportViewModel());
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.done.setOnClickListener(this::onDoneClicked);
        binding.viewEmail.setOnClickListener(this::onViewEmailClicked);

        return binding.getRoot();
    }

    private void onViewEmailClicked(final View view) {
        final AutocryptExportViewModel.Message message =
                getAutocryptExportViewModel().getMessage().getValue();
        if (message == null) {
            throw new IllegalStateException("Message was null");
        }
        final Intent intent =
                LttrsActivity.viewIntent(requireActivity(), message.tag, message.threadId);
        startActivity(intent);
    }

    private void onDoneClicked(final View view) {
        requireActivity().finish();
    }
}
