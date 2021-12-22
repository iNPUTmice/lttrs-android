/*
 * Copyright 2019-2021 Daniel Gultsch
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
import com.google.common.base.Preconditions;

public class OffsetListUpdateCallback<VH extends RecyclerView.ViewHolder>
        implements ListUpdateCallback {

    private final AdapterListUpdateCallback adapterCallback;
    private final int offset;
    private boolean isOffsetVisible = true;

    public OffsetListUpdateCallback(
            final RecyclerView.Adapter<VH> adapter,
            final int offset,
            final boolean isOffsetVisible) {
        this(adapter, offset);
        this.isOffsetVisible = isOffsetVisible;
    }

    public OffsetListUpdateCallback(final RecyclerView.Adapter<VH> adapter, final int offset) {
        Preconditions.checkArgument(offset >= 0, "Offset can not be negative");
        this.adapterCallback = new AdapterListUpdateCallback(adapter);
        this.offset = offset;
    }

    public boolean isOffsetVisible() {
        return this.isOffsetVisible;
    }

    public void setOffsetVisible(final boolean offsetVisible) {
        if (this.isOffsetVisible == offsetVisible || offset == 0) {
            return;
        }
        this.isOffsetVisible = offsetVisible;
        if (offsetVisible) {
            adapterCallback.onInserted(0, offset);
        } else {
            adapterCallback.onRemoved(0, offset);
        }
    }

    @Override
    public void onInserted(int position, int count) {
        adapterCallback.onInserted(position + getCurrentOffset(), count);
    }

    @Override
    public void onRemoved(int position, int count) {
        adapterCallback.onRemoved(position + getCurrentOffset(), count);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
        adapterCallback.onMoved(fromPosition + getCurrentOffset(), toPosition + getCurrentOffset());
    }

    @Override
    public void onChanged(int position, int count, @Nullable Object payload) {
        adapterCallback.onChanged(position + getCurrentOffset(), count, payload);
    }

    public int getCurrentOffset() {
        return this.isOffsetVisible ? this.offset : 0;
    }
}
