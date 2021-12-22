package rs.ltt.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import rs.ltt.android.R;
import rs.ltt.android.databinding.ItemMailboxSelectableBinding;
import rs.ltt.android.entity.SelectableMailbox;

public class ChooseLabelsAdapter extends RecyclerView.Adapter<ChooseLabelsAdapter.LabelViewHolder> {

    private static final DiffUtil.ItemCallback<SelectableMailbox> DIFF_ITEM_CALLBACK =
            new DiffUtil.ItemCallback<SelectableMailbox>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull SelectableMailbox oldItem, @NonNull SelectableMailbox newItem) {
                    return oldItem.matches(newItem);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull SelectableMailbox oldItem, @NonNull SelectableMailbox newItem) {
                    return oldItem.equals(newItem);
                }
            };
    private final AsyncListDiffer<SelectableMailbox> differ =
            new AsyncListDiffer<>(this, DIFF_ITEM_CALLBACK);
    private OnSelectableMailboxClickListener onSelectableMailboxClickListener;

    @NonNull
    @Override
    public LabelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new LabelViewHolder(
                DataBindingUtil.inflate(
                        LayoutInflater.from(parent.getContext()),
                        R.layout.item_mailbox_selectable,
                        parent,
                        false));
    }

    @Override
    public void onBindViewHolder(@NonNull LabelViewHolder holder, int position) {
        final SelectableMailbox mailbox = differ.getCurrentList().get(position);
        holder.binding.setLabel(mailbox);
        holder.binding.item.setOnClickListener(
                v -> {
                    if (onSelectableMailboxClickListener != null) {
                        onSelectableMailboxClickListener.onSelectableMailboxClick(mailbox);
                    }
                });
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    public void submitList(final List<SelectableMailbox> list) {
        differ.submitList(list);
    }

    public void setOnSelectableMailboxClickListener(
            final OnSelectableMailboxClickListener listener) {
        this.onSelectableMailboxClickListener = listener;
    }

    public interface OnSelectableMailboxClickListener {
        void onSelectableMailboxClick(SelectableMailbox mailbox);
    }

    public static class LabelViewHolder extends RecyclerView.ViewHolder {

        private final ItemMailboxSelectableBinding binding;

        public LabelViewHolder(@NonNull ItemMailboxSelectableBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
