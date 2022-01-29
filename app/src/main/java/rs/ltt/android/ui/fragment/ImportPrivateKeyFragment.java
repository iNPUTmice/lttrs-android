/*
 * Copyright 2019 Daniel Gultsch
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rs.ltt.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.base.Strings;
import rs.ltt.android.R;
import rs.ltt.android.databinding.FragmentImportPrivateKeyBinding;
import rs.ltt.android.entity.AutocryptSetupMessage;
import rs.ltt.android.ui.widget.SetupCodeEntry;
import rs.ltt.android.util.ToolTips;
import rs.ltt.android.util.Touch;
import rs.ltt.autocrypt.client.header.PassphraseHint;

public class ImportPrivateKeyFragment extends AbstractSetupFragment {

    private static final String INSTANCE_STATE_SETUP_CODE = "setup-code";

    private SetupCodeEntry setupCodeEntry;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final FragmentImportPrivateKeyBinding binding =
                DataBindingUtil.inflate(
                        inflater, R.layout.fragment_import_private_key, container, false);
        binding.setSetupViewModel(setupViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        Touch.expandTouchArea(binding.requestHelp, 16);
        ToolTips.apply(binding.requestHelp);
        binding.requestHelp.setOnClickListener(this::requestHelp);
        binding.next.setOnClickListener(this::onNextClicked);
        binding.skip.setOnClickListener(this::onSkipClicked);
        this.setupCodeEntry = SetupCodeEntry.of(binding.setupCode);
        this.setupCodeEntry.setOnSetupCodeSubmitted(code -> setupViewModel.enterSetupCode(code));
        final AutocryptSetupMessage autocryptSetupMessage = this.setupViewModel.peekSetupMessage();
        if (autocryptSetupMessage != null) {
            binding.accountName.setText(autocryptSetupMessage.getAccount().getName());
            final PassphraseHint passphraseHint = autocryptSetupMessage.getPassphraseHint();
            if (savedInstanceState != null) {
                final String setupCode =
                        savedInstanceState.getString(
                                INSTANCE_STATE_SETUP_CODE,
                                Strings.nullToEmpty(passphraseHint.begin));
                this.setupCodeEntry.setSetupCode(setupCode);
            } else {
                this.setupCodeEntry.setSetupCode(Strings.nullToEmpty(passphraseHint.begin));
            }
            this.setupCodeEntry.requestFocus();
        } else {
            setupViewModel.nextPrivateKeyImport();
        }
        return binding.getRoot();
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(INSTANCE_STATE_SETUP_CODE, setupCodeEntry.getSetupCode());
    }

    private void onSkipClicked(View view) {
        setupViewModel.nextPrivateKeyImport();
    }

    private void onNextClicked(final View view) {
        setupViewModel.enterSetupCode(this.setupCodeEntry.getSetupCode());
    }

    private void requestHelp(final View view) {
        new MaterialAlertDialogBuilder(view.getContext())
                .setTitle(R.string.import_private_key_help_dialog_title)
                .setMessage(R.string.import_private_key_help_dialog_message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }
}
