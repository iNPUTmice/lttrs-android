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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.paging.AsyncPagedListDiffer;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collection;
import java.util.Set;

import rs.ltt.android.R;
import rs.ltt.android.databinding.ItemEmailBinding;
import rs.ltt.android.databinding.ItemEmailHeaderBinding;
import rs.ltt.android.entity.EmailComplete;
import rs.ltt.android.entity.ExpandedPosition;
import rs.ltt.android.entity.SubjectWithImportance;
import rs.ltt.android.ui.BindingAdapters;
import rs.ltt.android.util.Touch;

public class ThreadAdapter extends RecyclerView.Adapter<ThreadAdapter.AbstractThreadItemViewHolder> {

    private static final DiffUtil.ItemCallback<EmailComplete> ITEM_CALLBACK = new DiffUtil.ItemCallback<EmailComplete>() {

        @Override
        public boolean areItemsTheSame(@NonNull EmailComplete oldItem, @NonNull EmailComplete newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull EmailComplete oldItem, @NonNull EmailComplete newItem) {
            return false;
        }
    };

    private static final int ITEM_VIEW_TYPE = 1;
    private static final int HEADER_VIEW_TYPE = 2;
    private final Set<String> expandedItems;
    //we need this rather inconvenient setup instead of simply using PagedListAdapter to allow for
    //a header view. If we were to use the PagedListAdapter the item update callbacks wouldn't work.
    //The problem and the solution is described in this github issue: https://github.com/googlesamples/android-architecture-components/issues/375
    //additional documentation on how to implement a AsyncPagedListDiffer can be found here:
    //https://developer.android.com/reference/android/arch/paging/AsyncPagedListDiffer
    private final AsyncPagedListDiffer<EmailComplete> mDiffer = new AsyncPagedListDiffer<>(
            new OffsetListUpdateCallback<>(this, 1),
            new AsyncDifferConfig.Builder<>(ITEM_CALLBACK).build()
    );
    private SubjectWithImportance subjectWithImportance;
    private Boolean flagged;
    private OnFlaggedToggled onFlaggedToggled;
    private OnComposeActionTriggered onComposeActionTriggered;

    public ThreadAdapter(Set<String> expandedItems) {
        this.expandedItems = expandedItems;
    }

    @NonNull
    @Override
    public AbstractThreadItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ITEM_VIEW_TYPE) {
            return new ThreadItemViewHolder(DataBindingUtil.inflate(inflater, R.layout.item_email, parent, false));
        } else {
            return new ThreadHeaderViewHolder(DataBindingUtil.inflate(inflater, R.layout.item_email_header, parent, false));
        }

    }

    @Override
    public void onBindViewHolder(@NonNull AbstractThreadItemViewHolder holder, final int position) {
        if (holder instanceof ThreadHeaderViewHolder) {
            final ThreadHeaderViewHolder headerViewHolder = (ThreadHeaderViewHolder) holder;
            headerViewHolder.binding.setSubject(subjectWithImportance);
            headerViewHolder.binding.setFlagged(flagged);
            headerViewHolder.binding.starToggle.setOnClickListener(v -> {
                if (onFlaggedToggled != null && subjectWithImportance != null) {
                    final boolean target = !flagged;
                    BindingAdapters.setIsFlagged(headerViewHolder.binding.starToggle, target);
                    onFlaggedToggled.onFlaggedToggled(subjectWithImportance.threadId, target);
                }
            });
            Touch.expandTouchArea(headerViewHolder.binding.starToggle, 16);
        } else if (holder instanceof ThreadItemViewHolder) {
            final ThreadItemViewHolder itemViewHolder = (ThreadItemViewHolder) holder;
            final EmailComplete email = mDiffer.getItem(position - 1);
            final boolean lastEmail = mDiffer.getItemCount() == position;
            final boolean expanded = email != null && expandedItems.contains(email.id);
            itemViewHolder.binding.setExpanded(expanded);
            itemViewHolder.binding.setEmail(email);
            itemViewHolder.binding.divider.setVisibility(lastEmail ? View.GONE : View.VISIBLE);
            if (expanded) {
                Touch.expandTouchArea(itemViewHolder.binding.moreOptions, 8);
            } else {
                itemViewHolder.binding.header.setTouchDelegate(null);
            }
            itemViewHolder.binding.header.setOnClickListener(v -> {
                if (expandedItems.contains(email.id)) {
                    expandedItems.remove(email.id);
                } else {
                    expandedItems.add(email.id);
                }
                notifyItemChanged(position);
            });
            itemViewHolder.binding.edit.setOnClickListener(v -> onComposeActionTriggered.onEditDraft(email.id));
            itemViewHolder.binding.replyAll.setOnClickListener(v -> onComposeActionTriggered.onReplyAll(email.id));
        }

    }

    @Override
    public int getItemCount() {
        return mDiffer.getItemCount() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? HEADER_VIEW_TYPE : ITEM_VIEW_TYPE;
    }

    public void setSubjectWithImportance(SubjectWithImportance subjectWithImportance) {
        this.subjectWithImportance = subjectWithImportance;
        //TODO notify only if actually changed
        notifyItemChanged(0);
    }

    public void setFlagged(Boolean flagged) {
        this.flagged = flagged;
        notifyItemChanged(0);
    }

    public void setOnFlaggedToggledListener(OnFlaggedToggled listener) {
        this.onFlaggedToggled = listener;
    }

    public void setOnComposeActionTriggeredListener(OnComposeActionTriggered listener) {
        this.onComposeActionTriggered = listener;
    }

    public void submitList(PagedList<EmailComplete> pagedList, Runnable runnable) {
        mDiffer.submitList(pagedList, runnable);
    }

    public void expand(Collection<ExpandedPosition> positions) {
        for (ExpandedPosition expandedPosition : positions) {
            this.expandedItems.add(expandedPosition.emailId);
        }
    }

    public boolean isInitialLoad() {
        final PagedList<EmailComplete> currentList = mDiffer.getCurrentList();
        return currentList == null || currentList.isEmpty();

    }

    static class AbstractThreadItemViewHolder extends RecyclerView.ViewHolder {


        AbstractThreadItemViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class ThreadItemViewHolder extends AbstractThreadItemViewHolder {

        private final ItemEmailBinding binding;

        ThreadItemViewHolder(@NonNull ItemEmailBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class ThreadHeaderViewHolder extends AbstractThreadItemViewHolder {

        private final ItemEmailHeaderBinding binding;

        ThreadHeaderViewHolder(@NonNull ItemEmailHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }


}
