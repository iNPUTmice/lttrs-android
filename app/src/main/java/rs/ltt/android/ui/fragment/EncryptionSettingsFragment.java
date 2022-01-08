package rs.ltt.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import rs.ltt.android.R;
import rs.ltt.android.databinding.FragmentEncryptionSettingsBinding;
import rs.ltt.android.ui.BindingAdapters;
import rs.ltt.android.ui.MaterialAlertDialogs;
import rs.ltt.android.ui.activity.AutocryptExportActivity;
import rs.ltt.android.ui.model.AutocryptViewModel;
import rs.ltt.android.util.Event;
import rs.ltt.autocrypt.client.header.EncryptionPreference;

public class EncryptionSettingsFragment extends AbstractAccountManagerFragment {

    private AutocryptViewModel autocryptViewModel;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final EncryptionSettingsFragmentArgs arguments =
                EncryptionSettingsFragmentArgs.fromBundle(requireArguments());
        final long accountId = arguments.getId();
        FragmentEncryptionSettingsBinding binding =
                DataBindingUtil.inflate(
                        inflater, R.layout.fragment_encryption_settings, container, false);

        binding.setLifecycleOwner(getViewLifecycleOwner());

        final ViewModelProvider viewModelProvider =
                new ViewModelProvider(
                        getViewModelStore(),
                        new AutocryptViewModel.Factory(
                                requireActivity().getApplication(), accountId));
        this.autocryptViewModel = viewModelProvider.get(AutocryptViewModel.class);
        binding.setViewModel(this.autocryptViewModel);
        binding.encryptionPreference.setOnCheckedChangeListener(
                this::onEncryptionPreferenceChanged);
        this.autocryptViewModel
                .getEncryptionPreference()
                .observe(
                        getViewLifecycleOwner(),
                        encryptionPreference -> {
                            if (encryptionPreference == EncryptionPreference.MUTUAL) {
                                BindingAdapters.setChecked(binding.mutual, true);
                                BindingAdapters.flagInitialValueSet(binding.noPreference);
                            } else if (encryptionPreference == EncryptionPreference.NO_PREFERENCE) {
                                BindingAdapters.setChecked(binding.noPreference, true);
                                BindingAdapters.flagInitialValueSet(binding.mutual);
                            }
                        });

        this.autocryptViewModel
                .getErrorMessage()
                .observe(getViewLifecycleOwner(), this::onErrorMessage);

        binding.transferSecretKey.setOnClickListener(this::onTransferSecretKey);

        return binding.getRoot();
    }

    private void onTransferSecretKey(final View view) {
        AutocryptExportActivity.launch(requireActivity(), autocryptViewModel.getAccountId());
    }

    private void onErrorMessage(Event<String> event) {
        MaterialAlertDialogs.error(requireActivity(), event);
    }

    private void onEncryptionPreferenceChanged(final RadioGroup radioGroup, final @IdRes int id) {
        if (id == R.id.mutual) {
            this.autocryptViewModel.setEncryptionPreference(EncryptionPreference.MUTUAL);
        } else if (id == R.id.no_preference) {
            this.autocryptViewModel.setEncryptionPreference(EncryptionPreference.NO_PREFERENCE);
        }
    }
}
