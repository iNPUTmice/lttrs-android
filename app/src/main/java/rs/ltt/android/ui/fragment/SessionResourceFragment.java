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
import rs.ltt.android.R;
import rs.ltt.android.databinding.FragmentSessionResourceBinding;
import rs.ltt.android.util.ToolTips;
import rs.ltt.android.util.Touch;

public class SessionResourceFragment extends AbstractSetupFragment {

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final FragmentSessionResourceBinding binding =
                DataBindingUtil.inflate(
                        inflater, R.layout.fragment_session_resource, container, false);
        binding.setSetupViewModel(setupViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        Touch.expandTouchArea(binding.requestHelp, 16);
        ToolTips.apply(binding.requestHelp);
        binding.requestHelp.setOnClickListener(this::requestHelp);
        return binding.getRoot();
    }

    private void requestHelp(final View view) {
        new MaterialAlertDialogBuilder(view.getContext())
                .setTitle(R.string.session_resource_help_dialog_title)
                .setMessage(R.string.session_resource_help_dialog_message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }
}
