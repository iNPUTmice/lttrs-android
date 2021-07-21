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

package rs.ltt.android.ui.adapter;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AdapterListUpdateCallback;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;

//TODO toggle offset
public class OffsetListUpdateCallback<VH extends RecyclerView.ViewHolder> implements ListUpdateCallback {

    private final AdapterListUpdateCallback adapterCallback;
    private final int offset;

    public OffsetListUpdateCallback(RecyclerView.Adapter<VH> adapter, int offset) {
        this.adapterCallback = new AdapterListUpdateCallback(adapter);
        this.offset = offset;
    }

    @Override
    public void onInserted(int position, int count) {
        adapterCallback.onInserted(position + offset, count);
    }

    @Override
    public void onRemoved(int position, int count) {
        adapterCallback.onRemoved(position + offset, count);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
        adapterCallback.onMoved(fromPosition + offset, toPosition + offset);
    }

    @Override
    public void onChanged(int position, int count, @Nullable Object payload) {
        adapterCallback.onChanged(position + offset, count, payload);
    }

}
