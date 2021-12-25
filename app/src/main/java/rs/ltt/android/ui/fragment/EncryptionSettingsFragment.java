package rs.ltt.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import rs.ltt.android.R;
import rs.ltt.android.databinding.FragmentEncryptionSettingsBinding;

public class EncryptionSettingsFragment extends AbstractAccountManagerFragment {

    private FragmentEncryptionSettingsBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        this.binding =
                DataBindingUtil.inflate(
                        inflater, R.layout.fragment_encryption_settings, container, false);

        return this.binding.getRoot();
    }
}
