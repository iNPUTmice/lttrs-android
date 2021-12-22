/*
 * Copyright 2020 Daniel Gultsch
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

package rs.ltt.android.ui;

import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import java.lang.ref.WeakReference;

public class WeakActionModeCallback implements ActionMode.Callback {

    private final WeakReference<ActionMode.Callback> weakCallbackReference;

    public WeakActionModeCallback(final @NonNull ActionMode.Callback callback) {
        this.weakCallbackReference = new WeakReference<>(callback);
    }

    @Override
    public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
        final ActionMode.Callback callback = weakCallbackReference.get();
        return callback != null && callback.onCreateActionMode(mode, menu);
    }

    @Override
    public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
        final ActionMode.Callback callback = weakCallbackReference.get();
        return callback != null && callback.onPrepareActionMode(mode, menu);
    }

    @Override
    public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
        final ActionMode.Callback callback = weakCallbackReference.get();
        return callback != null && callback.onActionItemClicked(mode, item);
    }

    @Override
    public void onDestroyActionMode(final ActionMode mode) {
        final ActionMode.Callback callback = weakCallbackReference.get();
        if (callback != null) {
            callback.onDestroyActionMode(mode);
        }
    }
}
