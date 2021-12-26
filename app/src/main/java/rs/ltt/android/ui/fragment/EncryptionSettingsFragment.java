package rs.ltt.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import rs.ltt.android.R;
import rs.ltt.android.databinding.FragmentEncryptionSettingsBinding;
import rs.ltt.android.ui.model.AccountViewModel;
import rs.ltt.android.ui.model.AutocryptViewModel;
import rs.ltt.autocrypt.client.header.EncryptionPreference;

public class EncryptionSettingsFragment extends AbstractAccountManagerFragment {

    private FragmentEncryptionSettingsBinding binding;
    private AutocryptViewModel autocryptViewModel;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final EncryptionSettingsFragmentArgs arguments = EncryptionSettingsFragmentArgs.fromBundle(requireArguments());
        final long accountId = arguments.getId();
        this.binding =
                DataBindingUtil.inflate(
                        inflater, R.layout.fragment_encryption_settings, container, false);

        this.binding.setLifecycleOwner(this);

        final ViewModelProvider viewModelProvider =
                new ViewModelProvider(
                        getViewModelStore(),
                        new AutocryptViewModel.Factory(
                                requireActivity().getApplication(), accountId));
        this.autocryptViewModel = viewModelProvider.get(AutocryptViewModel.class);
        this.binding.setViewModel(this.autocryptViewModel);
        this.binding.encryptionPreference.setOnCheckedChangeListener(this::onEncryptionPreferenceChanged);
        this.autocryptViewModel.getEncryptionPreference().observe(getViewLifecycleOwner(), encryptionPreference -> {
            if (encryptionPreference == EncryptionPreference.MUTUAL) {
                binding.mutual.setChecked(true);
            } else if (encryptionPreference == EncryptionPreference.NO_PREFERENCE) {
                binding.noPreference.setChecked(true);
            }
        });

        return this.binding.getRoot();
    }

    private void onEncryptionPreferenceChanged(final RadioGroup radioGroup, final @IdRes int id) {
        if (id == R.id.mutual) {
            this.autocryptViewModel.setEncryptionPreference(EncryptionPreference.MUTUAL);
        } else if (id == R.id.no_preference) {
            this.autocryptViewModel.setEncryptionPreference(EncryptionPreference.NO_PREFERENCE);
        }
    }

    @Override
    public void onDestroyView() {
        nullReferences();
        super.onDestroyView();
    }

    private void nullReferences() {
        this.autocryptViewModel = null;
        this.binding = null;
    }
}
