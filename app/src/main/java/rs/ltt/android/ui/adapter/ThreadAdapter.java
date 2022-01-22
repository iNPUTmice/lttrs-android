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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.helper.widget.Flow;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.paging.AsyncPagedListDiffer;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import rs.ltt.android.R;
import rs.ltt.android.databinding.ItemAttachmentBinding;
import rs.ltt.android.databinding.ItemEmailBinding;
import rs.ltt.android.databinding.ItemEmailHeaderBinding;
import rs.ltt.android.databinding.ItemLabelBinding;
import rs.ltt.android.entity.EmailBodyPartEntity;
import rs.ltt.android.entity.EmailWithBodies;
import rs.ltt.android.entity.EncryptionStatus;
import rs.ltt.android.entity.ExpandedPosition;
import rs.ltt.android.entity.MailboxWithRoleAndName;
import rs.ltt.android.entity.SubjectWithImportance;
import rs.ltt.android.ui.BindingAdapters;
import rs.ltt.android.ui.preview.AttachmentPreview;
import rs.ltt.android.util.ToolTips;
import rs.ltt.android.util.Touch;
import rs.ltt.jmap.mua.util.Label;

public class ThreadAdapter
        extends RecyclerView.Adapter<ThreadAdapter.AbstractThreadItemViewHolder> {

    private static final DiffUtil.ItemCallback<EmailWithBodies> ITEM_CALLBACK =
            new DiffUtil.ItemCallback<EmailWithBodies>() {

                @Override
                public boolean areItemsTheSame(
                        @NonNull EmailWithBodies oldItem, @NonNull EmailWithBodies newItem) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull EmailWithBodies oldItem, @NonNull EmailWithBodies newItem) {
                    // TODO this can probably be reduced to check if id and isDraft equals. Because
                    // isDraft(()
                    // is the only (displayed) thing that is realistically going to change in an
                    // otherwise immutable email
                    return oldItem.equals(newItem);
                }
            };

    private static final int ITEM_VIEW_TYPE = 1;
    private static final int HEADER_VIEW_TYPE = 2;
    private final long accountId;
    private final Set<String> expandedItems;
    // we need this rather inconvenient setup instead of simply using PagedListAdapter to allow for
    // a header view. If we were to use the PagedListAdapter the item update callbacks wouldn't
    // work.
    // The problem and the solution is described in this github issue:
    // https://github.com/googlesamples/android-architecture-components/issues/375
    // additional documentation on how to implement a AsyncPagedListDiffer can be found here:
    // https://developer.android.com/reference/android/arch/paging/AsyncPagedListDiffer
    private final AsyncPagedListDiffer<EmailWithBodies> mDiffer =
            new AsyncPagedListDiffer<>(
                    new OffsetListUpdateCallback<>(this, 1),
                    new AsyncDifferConfig.Builder<>(ITEM_CALLBACK).build());
    private SubjectWithImportance subjectWithImportance;
    private List<MailboxWithRoleAndName> labels = Collections.emptyList();
    private Boolean flagged;
    private OnFlaggedToggled onFlaggedToggled;
    private OnComposeActionTriggered onComposeActionTriggered;
    private OnAttachmentActionTriggered onAttachmentActionTriggered;
    private OnEncryptionActionTriggered onEncryptionActionTriggered;

    public ThreadAdapter(final long accountId, final Set<String> expandedItems) {
        this.accountId = accountId;
        this.expandedItems = expandedItems;
    }

    private static boolean skip(
            final LinearLayout attachments, final List<EmailBodyPartEntity> emailAttachments) {
        final Object tag = attachments.getTag();
        if (tag instanceof Integer) {
            final int hashCode = (Integer) tag;
            return hashCode == emailAttachments.hashCode();
        }
        return false;
    }

    @NonNull
    @Override
    public AbstractThreadItemViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ITEM_VIEW_TYPE) {
            return new ThreadItemViewHolder(
                    DataBindingUtil.inflate(inflater, R.layout.item_email, parent, false));
        } else {
            return new ThreadHeaderViewHolder(
                    DataBindingUtil.inflate(inflater, R.layout.item_email_header, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull AbstractThreadItemViewHolder holder, final int position) {
        if (holder instanceof ThreadHeaderViewHolder) {
            onBindViewHolder((ThreadHeaderViewHolder) holder);
        } else if (holder instanceof ThreadItemViewHolder) {
            onBindViewHolder((ThreadItemViewHolder) holder, position);
        }
    }

    private void onBindViewHolder(@NonNull final ThreadHeaderViewHolder headerViewHolder) {
        headerViewHolder.binding.setSubject(subjectWithImportance);
        headerViewHolder.binding.setFlagged(flagged);
        headerViewHolder.binding.starToggle.setOnClickListener(
                v -> {
                    if (onFlaggedToggled != null && subjectWithImportance != null) {
                        final boolean target = !flagged;
                        BindingAdapters.setIsFlagged(headerViewHolder.binding.starToggle, target);
                        onFlaggedToggled.onFlaggedToggled(subjectWithImportance.threadId, target);
                    }
                });
        if (this.labels.isEmpty()) {
            headerViewHolder.binding.labels.setVisibility(View.GONE);
        } else {
            headerViewHolder.binding.labels.setVisibility(View.VISIBLE);
            updateLabels(headerViewHolder.binding.labels, headerViewHolder.binding.flowWidget);
        }
        Touch.expandTouchArea(headerViewHolder.binding.starToggle, 16);
    }

    private void updateLabels(final ConstraintLayout labels, final Flow flowWidget) {
        if (skip(labels)) {
            return;
        }
        labels.removeViews(1, labels.getChildCount() - 1);
        final LayoutInflater inflater = LayoutInflater.from(labels.getContext());
        final int[] ids = new int[this.labels.size()];
        int i = 0;
        for (final Label label : this.labels) {
            final ItemLabelBinding itemLabelBinding =
                    DataBindingUtil.inflate(inflater, R.layout.item_label, labels, false);
            itemLabelBinding.setLabel(label);
            final int id = ViewCompat.generateViewId();
            itemLabelBinding.getRoot().setId(id);
            labels.addView(itemLabelBinding.getRoot());
            ids[i] = id;
            ++i;
        }
        flowWidget.setReferencedIds(ids);
        labels.setTag(this.labels);
    }

    private boolean skip(final ConstraintLayout labels) {
        final Object tag = labels.getTag();
        if (tag instanceof List) {
            final List<?> current = (List<?>) labels.getTag();
            return current.equals(this.labels);
        }
        return false;
    }

    private void onBindViewHolder(
            @NonNull final ThreadItemViewHolder itemViewHolder, final int position) {
        final EmailWithBodies email = mDiffer.getItem(position - 1);
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
        itemViewHolder.binding.header.setOnClickListener(
                v -> {
                    if (expandedItems.contains(email.id)) {
                        expandedItems.remove(email.id);
                    } else {
                        expandedItems.add(email.id);
                    }
                    notifyItemChanged(position);
                });
        itemViewHolder.binding.edit.setOnClickListener(
                v -> onComposeActionTriggered.onEditDraft(email.id));
        itemViewHolder.binding.replyAll.setOnClickListener(
                v -> onComposeActionTriggered.onReplyAll(email.id));
        itemViewHolder.binding.moreOptions.setOnClickListener(v -> onMoreOptions(v, email));
        updateAttachments(itemViewHolder.binding.attachments, email.getAttachments());
    }

    private void onMoreOptions(final View view, final EmailWithBodies email) {
        final PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        popupMenu.inflate(R.menu.email_item_more_options);
        final MenuItem decryptEmail = popupMenu.getMenu().findItem(R.id.decrypt_email);
        decryptEmail.setVisible(email.getEncryptionStatus() == EncryptionStatus.FAILED);
        popupMenu.setOnMenuItemClickListener(
                menuItem -> {
                    if (menuItem.getItemId() == R.id.reply) {
                        onComposeActionTriggered.onReply(email.id);
                        return true;
                    } else if (menuItem.getItemId() == R.id.decrypt_email) {
                        onEncryptionActionTriggered.onDecryptTriggered(email.id);
                        return true;
                    } else {
                        return false;
                    }
                });
        popupMenu.show();
    }

    private void updateAttachments(
            final LinearLayout attachments, final List<EmailBodyPartEntity> emailAttachments) {
        if (skip(attachments, emailAttachments)) {
            return;
        }
        final LayoutInflater layoutInflater = LayoutInflater.from(attachments.getContext());
        attachments.removeAllViews();
        for (final EmailBodyPartEntity attachment : emailAttachments) {
            attachments.addView(getAttachmentView(layoutInflater, attachments, attachment));
        }
        attachments.setTag(emailAttachments.hashCode());
    }

    private View getAttachmentView(
            final LayoutInflater layoutInflater,
            final LinearLayout attachments,
            final EmailBodyPartEntity attachment) {
        final ItemAttachmentBinding binding =
                DataBindingUtil.inflate(
                        layoutInflater, R.layout.item_attachment, attachments, false);
        binding.setAttachment(attachment);
        binding.getRoot()
                .setOnClickListener(
                        v ->
                                Objects.requireNonNull(
                                                onAttachmentActionTriggered,
                                                "Attachment Action listener not set")
                                        .onOpenTriggered(attachment.emailId, attachment));
        binding.action.setOnClickListener(
                v ->
                        Objects.requireNonNull(
                                        onAttachmentActionTriggered,
                                        "Attachment Action listener not set")
                                .onActionTriggered(attachment.emailId, attachment));
        AttachmentPreview.of(binding.preview, accountId, attachment).load();
        binding.action.setContentDescription(
                binding.action.getContext().getString(R.string.download_attachment));
        ToolTips.apply(binding.action);
        binding.getRoot().setId(ViewCompat.generateViewId());
        return binding.getRoot();
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
        // TODO notify only if actually changed
        notifyItemChanged(0);
    }

    public void setFlagged(Boolean flagged) {
        this.flagged = flagged;
        notifyItemChanged(0);
    }

    public void setLabels(final List<MailboxWithRoleAndName> labels) {
        final boolean unchanged = this.labels.equals(labels);
        this.labels = labels;
        if (unchanged) {
            return;
        }
        notifyItemChanged(0);
    }

    public void setOnFlaggedToggledListener(OnFlaggedToggled listener) {
        this.onFlaggedToggled = listener;
    }

    public void setOnComposeActionTriggeredListener(OnComposeActionTriggered listener) {
        this.onComposeActionTriggered = listener;
    }

    public void setOnAttachmentActionTriggered(OnAttachmentActionTriggered listener) {
        this.onAttachmentActionTriggered = listener;
    }

    public void setOnEncryptionActionTriggered(final OnEncryptionActionTriggered listener) {
        this.onEncryptionActionTriggered = listener;
    }

    public void submitList(PagedList<EmailWithBodies> pagedList, Runnable runnable) {
        mDiffer.submitList(pagedList, runnable);
    }

    public void expand(Collection<ExpandedPosition> positions) {
        for (ExpandedPosition expandedPosition : positions) {
            this.expandedItems.add(expandedPosition.emailId);
        }
    }

    public boolean isInitialLoad() {
        final PagedList<EmailWithBodies> currentList = mDiffer.getCurrentList();
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
            ToolTips.apply(binding.replyAll);
            ToolTips.apply(binding.forward);
            ToolTips.apply(binding.moreOptions);
            ToolTips.apply(binding.edit);
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
