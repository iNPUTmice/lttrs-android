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

package rs.ltt.android.util;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import java.util.Collections;
import java.util.List;

public class CombinedListsLiveData<T, U> extends MediatorLiveData<Pair<List<T>, List<U>>> {
    private List<T> ts = Collections.emptyList();
    private List<U> us = Collections.emptyList();

    public CombinedListsLiveData(
            final LiveData<List<T>> tLiveData, final LiveData<List<U>> uLiveData) {
        this.setValue(Pair.create(this.ts, this.us));
        this.addSource(
                tLiveData,
                (ts) -> {
                    if (ts != null) {
                        this.ts = ts;
                    }
                    this.setValue(Pair.create(ts, us));
                });
        this.addSource(
                uLiveData,
                (us) -> {
                    if (us != null) {
                        this.us = us;
                    }
                    this.setValue(Pair.create(ts, us));
                });
    }
}
