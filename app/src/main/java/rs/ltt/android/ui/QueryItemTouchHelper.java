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

package rs.ltt.android.ui;

import android.graphics.Canvas;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import rs.ltt.android.R;
import rs.ltt.android.ui.adapter.ThreadOverviewAdapter;

public class QueryItemTouchHelper extends ItemTouchHelper.SimpleCallback {

    private final OnQueryItemSwipe onQueryItemSwipe;

    public QueryItemTouchHelper(OnQueryItemSwipe onQueryItemSwipe) {
        super(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT);
        this.onQueryItemSwipe = onQueryItemSwipe;
    }

    @Override
    public boolean onMove(
            @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder,
            @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public int getSwipeDirs(
            @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof ThreadOverviewAdapter.ThreadOverviewViewHolder) {
            final ThreadOverviewAdapter.ThreadOverviewViewHolder threadOverviewViewHolder =
                    (ThreadOverviewAdapter.ThreadOverviewViewHolder) viewHolder;
            if (onQueryItemSwipe != null) {
                final Swipable swipable =
                        onQueryItemSwipe.onQueryItemSwipe(viewHolder.getAdapterPosition());
                if (swipable == Swipable.NO) {
                    return 0;
                }
                @DrawableRes final int resource;
                switch (swipable) {
                    case ARCHIVE:
                        resource = R.drawable.ic_archive_white_24dp;
                        break;
                    case REMOVE_FLAGGED:
                        resource = R.drawable.ic_star_border_white_24dp;
                        break;
                    default:
                        resource = R.drawable.ic_label_off_white_24dp;
                        break;
                }
                threadOverviewViewHolder.binding.endSwipeActionIndicator.setImageResource(resource);
                threadOverviewViewHolder.binding.startSwipeActionIndicator.setImageResource(
                        resource);
                return ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT;
            }
        }
        return 0;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        if (onQueryItemSwipe != null) {
            onQueryItemSwipe.onQueryItemSwiped(viewHolder);
        }
    }

    @Override
    public void onChildDraw(
            @NonNull Canvas c,
            @Nullable RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder,
            float dX,
            float dY,
            int actionState,
            boolean isCurrentlyActive) {
        if (viewHolder instanceof ThreadOverviewAdapter.ThreadOverviewViewHolder) {
            final ThreadOverviewAdapter.ThreadOverviewViewHolder threadOverviewViewHolder =
                    (ThreadOverviewAdapter.ThreadOverviewViewHolder) viewHolder;
            getDefaultUIUtil()
                    .onDraw(
                            c,
                            recyclerView,
                            threadOverviewViewHolder.binding.foreground,
                            dX,
                            dY,
                            actionState,
                            isCurrentlyActive);
        }
    }

    @Override
    public void clearView(
            @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof ThreadOverviewAdapter.ThreadOverviewViewHolder) {
            ThreadOverviewAdapter.ThreadOverviewViewHolder threadOverviewViewHolder =
                    (ThreadOverviewAdapter.ThreadOverviewViewHolder) viewHolder;
            getDefaultUIUtil().clearView(threadOverviewViewHolder.binding.foreground);
        }
    }

    public enum Swipable {
        NO,
        ARCHIVE,
        REMOVE_LABEL,
        REMOVE_FLAGGED
    }

    public interface OnQueryItemSwipe {
        Swipable onQueryItemSwipe(int position);

        void onQueryItemSwiped(@NonNull RecyclerView.ViewHolder viewHolder);
    }
}
