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

package rs.ltt.android.ui.model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class ThreadViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final long accountId;
    private final String threadId;
    private final String label;

    public ThreadViewModelFactory(final Application application,
                                  final long accountId,
                                  final String threadId,
                                  final String label) {
        this.application = application;
        this.accountId = accountId;
        this.threadId = threadId;
        this.label = label;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return modelClass.cast(new ThreadViewModel(application, accountId, threadId, label));
    }
}
