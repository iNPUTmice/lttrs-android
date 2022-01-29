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
import rs.ltt.android.R;
import rs.ltt.android.databinding.FragmentPasswordBinding;

public class PasswordFragment extends AbstractSetupFragment {

    FragmentPasswordBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        this.binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_password, container, false);
        binding.setSetupViewModel(setupViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.password.requestFocus();
        return binding.getRoot();
    }
}
