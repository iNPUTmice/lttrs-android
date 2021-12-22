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

import android.app.Activity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import rs.ltt.android.ui.ThreadModifier;
import rs.ltt.android.ui.activity.LttrsActivity;
import rs.ltt.android.ui.model.LttrsViewModel;

abstract class AbstractLttrsFragment extends Fragment {

    LttrsViewModel getLttrsViewModel() {
        final ViewModelProvider viewModelProvider =
                new ViewModelProvider(requireActivity(), getDefaultViewModelProviderFactory());
        return viewModelProvider.get(LttrsViewModel.class);
    }

    LttrsActivity requireLttrsActivity() {
        final Activity activity = getActivity();
        if (activity instanceof LttrsActivity) {
            return (LttrsActivity) activity;
        }
        throw new IllegalStateException("Fragment is not attached to LttrsActivity");
    }

    ThreadModifier getThreadModifier() {
        final Activity activity = requireActivity();
        if (activity instanceof ThreadModifier) {
            return (ThreadModifier) activity;
        }
        throw new IllegalStateException("Activity does not implement ThreadModifier");
    }

    NavController getNavController() {
        return requireLttrsActivity().getNavController();
    }
}
