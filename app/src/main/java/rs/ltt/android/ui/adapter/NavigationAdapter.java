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
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import rs.ltt.android.R;
import rs.ltt.android.databinding.ItemNavigationAccountBinding;
import rs.ltt.android.databinding.ItemNavigationAdditionalBinding;
import rs.ltt.android.databinding.ItemNavigationHeaderBinding;
import rs.ltt.android.databinding.ItemNavigationLabelBinding;
import rs.ltt.android.entity.AccountName;
import rs.ltt.android.entity.MailboxOverviewItem;
import rs.ltt.android.ui.AdditionalNavigationItem;
import rs.ltt.android.util.ConsistentColorGeneration;
import rs.ltt.jmap.mua.util.AccountUtil;
import rs.ltt.jmap.mua.util.KeywordLabel;
import rs.ltt.jmap.mua.util.Label;
import rs.ltt.jmap.mua.util.LabelWithCount;
import rs.ltt.jmap.mua.util.Navigable;

public class NavigationAdapter extends RecyclerView.Adapter<NavigationAdapter.AbstractNavigationItemViewHolder> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NavigationAdapter.class);

    private static final int HEADER_VIEW_TYPE = 1;
    private static final int LABEL_VIEW_TYPE = 2;
    private static final int ACCOUNT_VIEW_TYPE = 3;
    private static final int ADDITIONAL_VIEW_TYPE = 4;

    private static final DiffUtil.ItemCallback<Navigable> ITEM_CALLBACK = new DiffUtil.ItemCallback<Navigable>() {
        @Override
        public boolean areItemsTheSame(@NonNull Navigable oldItem, @NonNull Navigable newItem) {
            if (oldItem instanceof LabelWithCount && newItem instanceof LabelWithCount) {
                return same((LabelWithCount) oldItem, (LabelWithCount) newItem);
            }
            if (oldItem instanceof AccountName && newItem instanceof AccountName) {
                return ((AccountName) oldItem).id.equals(((AccountName) newItem).id);
            }
            if (oldItem instanceof AdditionalNavigationItem && newItem instanceof AdditionalNavigationItem) {
                return ((AdditionalNavigationItem) oldItem).type == ((AdditionalNavigationItem) newItem).type;
            }
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Navigable oldItem, @NonNull Navigable newItem) {
            return oldItem.equals(newItem);
        }
    };

    private final AsyncListDiffer<Navigable> mDiffer = new AsyncListDiffer<>(
            new OffsetListUpdateCallback<>(this, 1),
            new AsyncDifferConfig.Builder<>(ITEM_CALLBACK).build()
    );

    //current state
    private LabelWithCount selectedLabel = null;
    private boolean accountSelectionVisible = false;
    private AccountName accountName;

    //callbacks
    private OnLabelSelected onLabelSelected = null;
    private OnAccountViewToggled onAccountViewToggled = null;
    private OnAccountSelected onAccountSelected = null;
    private OnAdditionalNavigationItemSelected onAdditionalNavigationItemSelected = null;

    public NavigationAdapter() {
        super();
    }

    private static boolean same(final Label a, final Label b) {
        if (a instanceof MailboxOverviewItem && b instanceof MailboxOverviewItem) {
            return ((MailboxOverviewItem) a).getId().equals(((MailboxOverviewItem) b).getId());
        }
        if (a instanceof KeywordLabel && b instanceof KeywordLabel) {
            return a.getRole() != null && a.getRole().equals(b.getRole());
        }
        return false;
    }

    @NonNull
    @Override
    public AbstractNavigationItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, final int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        if (viewType == LABEL_VIEW_TYPE) {
            final ItemNavigationLabelBinding binding = DataBindingUtil.inflate(layoutInflater, R.layout.item_navigation_label, parent, false);
            return new LabelViewHolder(binding);
        } else if (viewType == ACCOUNT_VIEW_TYPE) {
            final ItemNavigationAccountBinding binding = DataBindingUtil.inflate(layoutInflater, R.layout.item_navigation_account, parent, false);
            return new AccountViewHolder(binding);
        } else if (viewType == ADDITIONAL_VIEW_TYPE) {
            final ItemNavigationAdditionalBinding binding = DataBindingUtil.inflate(layoutInflater, R.layout.item_navigation_additional, parent, false);
            return new AdditionalItemViewHolder(binding);
        } else {
            ItemNavigationHeaderBinding binding = DataBindingUtil.inflate(layoutInflater, R.layout.item_navigation_header, parent, false);
            return new NavigationHeaderViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull AbstractNavigationItemViewHolder abstractHolder, final int position) {
        if (abstractHolder instanceof NavigationHeaderViewHolder) {
            onBindViewHolder((NavigationHeaderViewHolder) abstractHolder);
            return;
        }
        final Navigable navigable = getItem(position);
        if (abstractHolder instanceof LabelViewHolder) {
            onBindViewHolder((LabelViewHolder) abstractHolder, (LabelWithCount) navigable);
            return;
        }
        if (abstractHolder instanceof AccountViewHolder) {
            onBindViewHolder((AccountViewHolder) abstractHolder, (AccountName) navigable);
            return;
        }
        if (abstractHolder instanceof AdditionalItemViewHolder) {
            onBindViewHolder((AdditionalItemViewHolder) abstractHolder, (AdditionalNavigationItem) navigable);
            return;
        }
        throw new IllegalStateException(String.format("Unable to bind %s", abstractHolder.getClass().getName()));
    }

    private void onBindViewHolder(final AdditionalItemViewHolder viewHolder, final AdditionalNavigationItem item) {
        @StringRes final int string;
        @DrawableRes final int icon;
        switch (item.type) {
            case MANAGE_ACCOUNT: {
                string = R.string.manage_accounts;
                icon = R.drawable.ic_baseline_manage_accounts_24;
            }
            break;
            case ADD_ACCOUNT: {
                string = R.string.add_another_account;
                icon = R.drawable.ic_baseline_add_account_24;
            }
            break;
            default:
                throw new IllegalStateException(String.format("Unable to draw %s", item.type));
        }
        viewHolder.binding.icon.setImageResource(icon);
        viewHolder.binding.label.setText(string);
        viewHolder.binding.item.setOnClickListener((v) -> {
            if (onAdditionalNavigationItemSelected != null) {
                onAdditionalNavigationItemSelected.onAdditionalNavigationItemSelected(item.type);
            }
        });
    }

    private void onBindViewHolder(final AccountViewHolder viewHolder, final AccountName accountName) {
        viewHolder.binding.setAccount(accountName);
        viewHolder.binding.item.setOnClickListener((v) -> {
            if (onAccountSelected != null) {
                onAccountSelected.onAccountSelected(accountName.id);
            }
        });
        viewHolder.binding.icon.setImageTintList(
                ColorStateList.valueOf(ConsistentColorGeneration.rgbFromKey(accountName.name))
        );
    }

    private void onBindViewHolder(final NavigationHeaderViewHolder viewHolder) {
        final @DrawableRes int imageResource;
        final @StringRes int imageDescriptionResource;
        if (accountSelectionVisible) {
            imageResource = R.drawable.ic_baseline_keyboard_arrow_up_24;
            imageDescriptionResource = R.string.close_account_selector;
        } else {
            imageResource = R.drawable.ic_keyboard_arrow_down_black_24dp;
            imageDescriptionResource = R.string.open_account_selector;
        }
        viewHolder.binding.toggle.setImageResource(imageResource);
        viewHolder.binding.toggle.setContentDescription(
                viewHolder.binding.getRoot().getContext().getString(imageDescriptionResource)
        );
        viewHolder.binding.toggle.setOnClickListener(v -> onAccountViewToggled.onAccountViewToggled());
        viewHolder.binding.wrapper.setOnClickListener(v -> onAccountViewToggled.onAccountViewToggled());
        if (this.accountName != null) {
            viewHolder.binding.name.setText(AccountUtil.printableName(this.accountName.name));
            viewHolder.binding.account.setText(this.accountName.name);
        }
    }

    private void onBindViewHolder(final LabelViewHolder viewHolder, final LabelWithCount label) {
        final Context context = viewHolder.binding.getRoot().getContext();
        viewHolder.binding.setLabel(label);
        viewHolder.binding.item.setOnClickListener(v -> {
            if (onLabelSelected != null) {
                onLabelSelected.onLabelSelected(label, same(label, this.selectedLabel));
            }
        });
        if (same(label, this.selectedLabel)) {
            viewHolder.binding.item.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_highlight));
            ImageViewCompat.setImageTintList(viewHolder.binding.icon, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
        } else {
            final TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            viewHolder.binding.item.setBackgroundResource(outValue.resourceId);
            ImageViewCompat.setImageTintList(viewHolder.binding.icon, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorSecondaryOnSurface)));
        }
    }

    public void setAccountInformation(final AccountName accountName) {
        this.accountName = accountName;
        notifyItemChanged(0);
    }

    public void setAccountSelectionVisible(final Boolean visible) {
        this.accountSelectionVisible = Boolean.TRUE.equals(visible);
        notifyItemChanged(0);
    }

    public void setSelectedLabel(final LabelWithCount label) {
        if ((label == null && this.selectedLabel == null) || (same(label, this.selectedLabel))) {
            return;
        }
        final int previous = getPosition(this.selectedLabel);
        final int position = getPosition(label);
        this.selectedLabel = label;
        if (previous != RecyclerView.NO_POSITION) {
            notifyItemChanged(previous);
        }
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position);
        }
    }

    private int getPosition(final LabelWithCount label) {
        if (label == null) {
            return RecyclerView.NO_POSITION;
        }
        final List<Navigable> items = mDiffer.getCurrentList();
        for (int i = 0; i < items.size(); ++i) {
            final Navigable navigable = items.get(i);
            if (navigable instanceof LabelWithCount && same(label, (LabelWithCount) navigable)) {
                return i + 1;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size() + 1;
    }

    @Override
    public int getItemViewType(final int position) {
        if (position == 0) {
            return HEADER_VIEW_TYPE;
        }
        final Navigable navigable = getItem(position);
        if (navigable instanceof LabelWithCount) {
            return LABEL_VIEW_TYPE;
        } else if (navigable instanceof AccountName) {
            return ACCOUNT_VIEW_TYPE;
        } else if (navigable instanceof AdditionalNavigationItem) {
            return ADDITIONAL_VIEW_TYPE;
        }
        throw new IllegalStateException(String.format("No view type found for %s", navigable.getClass().getSimpleName()));
    }

    private Navigable getItem(int position) {
        return this.mDiffer.getCurrentList().get(position - 1);
    }

    public void submitList(final List<Navigable> items) {
        this.mDiffer.submitList(items);
    }

    public void setOnLabelSelectedListener(final OnLabelSelected listener) {
        this.onLabelSelected = listener;
    }

    public void setOnAccountViewToggledListener(final OnAccountViewToggled listener) {
        this.onAccountViewToggled = listener;
    }

    public void setOnAccountSelected(final OnAccountSelected listener) {
        this.onAccountSelected = listener;
    }

    public void setOnAdditionalNavigationItemSelected(final OnAdditionalNavigationItemSelected listener) {
        this.onAdditionalNavigationItemSelected = listener;
    }

    public interface OnLabelSelected {
        void onLabelSelected(final Label label, boolean currentlySelected);
    }

    public interface OnAccountViewToggled {
        void onAccountViewToggled();
    }

    public interface OnAccountSelected {
        void onAccountSelected(long id);
    }

    public interface OnAdditionalNavigationItemSelected {
        void onAdditionalNavigationItemSelected(AdditionalNavigationItem.Type type);
    }

    static abstract class AbstractNavigationItemViewHolder extends RecyclerView.ViewHolder {

        AbstractNavigationItemViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class LabelViewHolder extends AbstractNavigationItemViewHolder {

        private final ItemNavigationLabelBinding binding;

        LabelViewHolder(@NonNull ItemNavigationLabelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class AccountViewHolder extends AbstractNavigationItemViewHolder {

        private final ItemNavigationAccountBinding binding;

        AccountViewHolder(@NonNull ItemNavigationAccountBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class NavigationHeaderViewHolder extends AbstractNavigationItemViewHolder {

        private final ItemNavigationHeaderBinding binding;

        NavigationHeaderViewHolder(@NonNull ItemNavigationHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class AdditionalItemViewHolder extends AbstractNavigationItemViewHolder {

        private final ItemNavigationAdditionalBinding binding;

        AdditionalItemViewHolder(ItemNavigationAdditionalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

    }
}
