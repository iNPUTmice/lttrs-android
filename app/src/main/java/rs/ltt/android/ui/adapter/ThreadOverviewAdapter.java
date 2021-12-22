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

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.paging.AsyncPagedListDiffer;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.R;
import rs.ltt.android.databinding.ItemThreadOverviewBinding;
import rs.ltt.android.databinding.ItemThreadOverviewEmptyActionBinding;
import rs.ltt.android.databinding.ItemThreadOverviewLoadingBinding;
import rs.ltt.android.entity.MailboxWithRoleAndName;
import rs.ltt.android.entity.ThreadOverviewItem;
import rs.ltt.android.ui.BindingAdapters;
import rs.ltt.android.ui.EmptyMailboxAction;
import rs.ltt.android.ui.MaterialBackgrounds;
import rs.ltt.android.util.Touch;

public class ThreadOverviewAdapter
        extends RecyclerView.Adapter<ThreadOverviewAdapter.AbstractThreadOverviewViewHolder> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadOverviewAdapter.class);
    private static final int THREAD_ITEM_VIEW_TYPE = 0;
    private static final int LOADING_ITEM_VIEW_TYPE = 1;
    private static final int EMPTY_MAILBOX_VIEW_TYPE = 2;
    private static final DiffUtil.ItemCallback<ThreadOverviewItem> ITEM_CALLBACK =
            new DiffUtil.ItemCallback<ThreadOverviewItem>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull ThreadOverviewItem oldItem, @NonNull ThreadOverviewItem newItem) {
                    return oldItem.threadId.equals(newItem.threadId);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull ThreadOverviewItem oldItem, @NonNull ThreadOverviewItem newItem) {
                    return oldItem.equals(newItem);
                }
            };

    private final OffsetListUpdateCallback<AbstractThreadOverviewViewHolder>
            offsetListUpdateCallback = new OffsetListUpdateCallback<>(this, 1, false);

    private final AsyncPagedListDiffer<ThreadOverviewItem> mDiffer =
            new AsyncPagedListDiffer<>(
                    offsetListUpdateCallback,
                    new AsyncDifferConfig.Builder<>(ITEM_CALLBACK).build());

    private boolean isLoading = false;
    private boolean initialLoadComplete = false;
    private OnFlaggedToggled onFlaggedToggled;
    private OnThreadClicked onThreadClicked;
    private OnSelectionToggled onSelectionToggled;
    private OnEmptyMailboxActionClicked onEmptyMailboxActionClicked;
    private Set<String> selectedThreads = Collections.emptySet();
    private Future<MailboxWithRoleAndName>
            importantMailbox; // TODO this needs to be a LiveData and needs to trigger a refresh
    // when changed
    private EmptyMailboxAction emptyMailboxAction = null;

    @NonNull
    @Override
    public AbstractThreadOverviewViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        if (viewType == THREAD_ITEM_VIEW_TYPE) {
            return new ThreadOverviewViewHolder(
                    DataBindingUtil.inflate(
                            layoutInflater, R.layout.item_thread_overview, parent, false));
        } else if (viewType == EMPTY_MAILBOX_VIEW_TYPE) {
            return new ThreadOverviewEmptyMailboxViewHolder(
                    DataBindingUtil.inflate(
                            layoutInflater,
                            R.layout.item_thread_overview_empty_action,
                            parent,
                            false));
        } else {
            return new ThreadOverviewLoadingViewHolder(
                    DataBindingUtil.inflate(
                            layoutInflater, R.layout.item_thread_overview_loading, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull AbstractThreadOverviewViewHolder holder, final int position) {
        if (holder instanceof ThreadOverviewLoadingViewHolder) {
            onBindViewHolder((ThreadOverviewLoadingViewHolder) holder);
        } else if (holder instanceof ThreadOverviewViewHolder) {
            onBindViewHolder((ThreadOverviewViewHolder) holder, position);
        } else if (holder instanceof ThreadOverviewEmptyMailboxViewHolder) {
            onBindViewHolder((ThreadOverviewEmptyMailboxViewHolder) holder);
        }
    }

    private void onBindViewHolder(final ThreadOverviewEmptyMailboxViewHolder holder) {
        final EmptyMailboxAction action = this.emptyMailboxAction;
        if (action == null) {
            return;
        }
        final Resources resources = holder.binding.getRoot().getContext().getResources();
        holder.binding.setAction(action);
        holder.binding.text.setText(
                resources.getQuantityString(
                        R.plurals.x_emails_in_trash, action.getItemCount(), action.getItemCount()));
        holder.binding.emptyMailbox.setOnClickListener(
                (v) -> {
                    if (onEmptyMailboxActionClicked != null) {
                        onEmptyMailboxActionClicked.onEmptyMailboxActionClicked(action);
                    }
                });
    }

    private void onBindViewHolder(
            final ThreadOverviewViewHolder threadOverviewHolder, final int position) {
        final ThreadOverviewItem item = getItem(position);
        if (item == null) {
            return;
        }
        final boolean selected = this.selectedThreads.contains(item.threadId);
        final Context context = threadOverviewHolder.binding.getRoot().getContext();
        threadOverviewHolder.binding.getRoot().setActivated(selected);
        threadOverviewHolder.setThread(item);
        threadOverviewHolder.binding.starToggle.setOnClickListener(
                v -> {
                    if (onSelectionToggled != null && selectedThreads.size() > 0) {
                        onSelectionToggled.onSelectionToggled(item.threadId, !selected);
                        return;
                    }
                    if (onFlaggedToggled != null) {
                        final boolean target = !item.showAsFlagged();
                        BindingAdapters.setIsFlagged(
                                threadOverviewHolder.binding.starToggle, target);
                        onFlaggedToggled.onFlaggedToggled(item.threadId, target);
                    }
                });
        Touch.expandTouchArea(threadOverviewHolder.binding.starToggle, 16);
        threadOverviewHolder.binding.foreground.setOnClickListener(
                v -> {
                    if (onSelectionToggled != null && selectedThreads.size() > 0) {
                        onSelectionToggled.onSelectionToggled(item.threadId, !selected);
                        return;
                    }
                    if (onThreadClicked != null) {
                        onThreadClicked.onThreadClicked(item, isImportant(item));
                    }
                });
        threadOverviewHolder.binding.foreground.setOnLongClickListener(
                v -> {
                    if (onSelectionToggled != null && selectedThreads.size() == 0) {
                        onSelectionToggled.onSelectionToggled(item.threadId, !selected);
                        return true;
                    }
                    return false;
                });
        threadOverviewHolder.binding.avatar.setOnClickListener(
                v -> {
                    if (onSelectionToggled != null) {
                        onSelectionToggled.onSelectionToggled(item.threadId, !selected);
                    }
                });
        if (selected) {
            threadOverviewHolder.binding.threadLayout.setBackground(
                    ContextCompat.getDrawable(context, R.drawable.selected_background));
        } else {
            threadOverviewHolder.binding.threadLayout.setBackgroundResource(
                    MaterialBackgrounds.getBackgroundResource(
                            context, android.R.attr.selectableItemBackground));
        }
    }

    public void notifyItemChanged(final String threadId) {
        final int position = getPosition(threadId);
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position + (offsetListUpdateCallback.isOffsetVisible() ? 1 : 0));
        }
    }

    private int getPosition(final String threadId) {
        final PagedList<ThreadOverviewItem> currentList = this.mDiffer.getCurrentList();
        if (currentList == null) {
            return RecyclerView.NO_POSITION;
        }
        final int offset = currentList.getPositionOffset();
        final List<ThreadOverviewItem> snapshot = currentList.snapshot();
        int i = 0;
        for (final ThreadOverviewItem item : snapshot) {
            if (item != null && threadId.equals(item.threadId)) {
                return offset + i;
            }
            ++i;
        }
        return RecyclerView.NO_POSITION;
    }

    private void onBindViewHolder(
            final ThreadOverviewLoadingViewHolder threadOverviewLoadingViewHolder) {
        threadOverviewLoadingViewHolder.binding.loading.setVisibility(
                isLoading() ? View.VISIBLE : View.GONE);
    }

    private MailboxWithRoleAndName getImportantMailbox() {
        if (this.importantMailbox != null && this.importantMailbox.isDone()) {
            try {
                return this.importantMailbox.get();
            } catch (Exception e) {
                return null;
            }
        } else {
            LOGGER.warn("Mailbox with IMPORTANT role was not available for rendering");
            return null;
        }
    }

    public void setImportantMailbox(Future<MailboxWithRoleAndName> importantMailbox) {
        this.importantMailbox = importantMailbox;
    }

    public boolean isImportant(ThreadOverviewItem item) {
        return item.isInMailbox(getImportantMailbox());
    }

    private void refreshLoadingIndicator(final boolean before) {
        if (before != isLoading()) {
            notifyItemChanged(mDiffer.getItemCount() + offsetListUpdateCallback.getCurrentOffset());
        }
    }

    private boolean isLoading() {
        return this.isLoading || !initialLoadComplete;
    }

    public void setLoading(final boolean loading) {
        final boolean before = isLoading();
        this.isLoading = loading;
        refreshLoadingIndicator(before);
    }

    public void submitList(final PagedList<ThreadOverviewItem> pagedList, final Runnable runnable) {
        final boolean before = isLoading();
        this.initialLoadComplete = true;
        if (pagedList != null && pagedList.size() == 0) {
            refreshLoadingIndicator(before);
        }
        this.mDiffer.submitList(pagedList, runnable);
    }

    public void setEmptyMailboxAction(final EmptyMailboxAction emptyMailboxAction) {
        this.emptyMailboxAction = emptyMailboxAction;
        this.offsetListUpdateCallback.setOffsetVisible(emptyMailboxAction != null);
    }

    public void setSelectedThreads(final Set<String> selectedThreads) {
        this.selectedThreads = selectedThreads;
    }

    public ThreadOverviewItem getItem(int position) {
        return this.mDiffer.getItem(
                offsetListUpdateCallback.isOffsetVisible() ? position - 1 : position);
    }

    @Override
    public int getItemViewType(int position) {
        if (offsetListUpdateCallback.isOffsetVisible() && position == 0) {
            return EMPTY_MAILBOX_VIEW_TYPE;
        } else if (position
                < mDiffer.getItemCount() + offsetListUpdateCallback.getCurrentOffset()) {
            return THREAD_ITEM_VIEW_TYPE;
        } else {
            return LOADING_ITEM_VIEW_TYPE;
        }
    }

    public void setOnFlaggedToggledListener(OnFlaggedToggled listener) {
        this.onFlaggedToggled = listener;
    }

    public void setOnThreadClickedListener(OnThreadClicked listener) {
        this.onThreadClicked = listener;
    }

    public void setOnEmptyMailboxActionClickedListener(OnEmptyMailboxActionClicked listener) {
        this.onEmptyMailboxActionClicked = listener;
    }

    public void setOnSelectionToggled(final OnSelectionToggled listener) {
        this.onSelectionToggled = listener;
    }

    @Override
    public int getItemCount() {
        return this.mDiffer.getItemCount() + 1 + this.offsetListUpdateCallback.getCurrentOffset();
    }

    public boolean isInitialLoad() {
        return !this.initialLoadComplete;
    }

    public PagedList<ThreadOverviewItem> getCurrentList() {
        return this.mDiffer.getCurrentList();
    }

    public interface OnThreadClicked {
        void onThreadClicked(ThreadOverviewItem threadOverviewItem, boolean important);
    }

    public interface OnEmptyMailboxActionClicked {
        void onEmptyMailboxActionClicked(EmptyMailboxAction action);
    }

    abstract static class AbstractThreadOverviewViewHolder extends RecyclerView.ViewHolder {

        AbstractThreadOverviewViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static class ThreadOverviewEmptyMailboxViewHolder
            extends AbstractThreadOverviewViewHolder {

        final ItemThreadOverviewEmptyActionBinding binding;

        ThreadOverviewEmptyMailboxViewHolder(
                @NonNull ItemThreadOverviewEmptyActionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public static class ThreadOverviewLoadingViewHolder extends AbstractThreadOverviewViewHolder {

        final ItemThreadOverviewLoadingBinding binding;

        ThreadOverviewLoadingViewHolder(@NonNull ItemThreadOverviewLoadingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public class ThreadOverviewViewHolder extends AbstractThreadOverviewViewHolder {

        public final ItemThreadOverviewBinding binding;

        ThreadOverviewViewHolder(@NonNull ItemThreadOverviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void setThread(final ThreadOverviewItem thread) {
            this.binding.setThread(thread);
            this.binding.setIsImportant(isImportant(thread));
        }
    }
}
