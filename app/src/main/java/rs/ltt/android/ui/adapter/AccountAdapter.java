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
import rs.ltt.android.databinding.ItemAccountBinding;
import rs.ltt.android.entity.AccountName;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {

    private static final DiffUtil.ItemCallback<AccountName> DIFF_ITEM_CALLBACK =
            new DiffUtil.ItemCallback<AccountName>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull AccountName oldItem, @NonNull AccountName newItem) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull AccountName oldItem, @NonNull AccountName newItem) {
                    return oldItem.equals(newItem);
                }
            };
    private final AsyncListDiffer<AccountName> differ =
            new AsyncListDiffer<>(this, DIFF_ITEM_CALLBACK);
    private OnAccountSelected onAccountSelected;

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AccountViewHolder(
                DataBindingUtil.inflate(
                        LayoutInflater.from(parent.getContext()),
                        R.layout.item_account,
                        parent,
                        false));
    }

    @Override
    public void onBindViewHolder(@NonNull final AccountViewHolder holder, final int position) {
        final AccountName account = this.differ.getCurrentList().get(position);
        holder.setAccount(account);
        holder.binding.wrapper.setOnClickListener(
                v -> {
                    if (onAccountSelected != null) {
                        onAccountSelected.onAccountSelected(account.getId());
                    }
                });
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    public void submitList(final List<AccountName> accounts) {
        this.differ.submitList(accounts);
    }

    public void setOnAccountSelected(final OnAccountSelected listener) {
        this.onAccountSelected = listener;
    }

    public interface OnAccountSelected {
        void onAccountSelected(final long id);
    }

    public static class AccountViewHolder extends RecyclerView.ViewHolder {

        private final ItemAccountBinding binding;

        public AccountViewHolder(final ItemAccountBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void setAccount(final AccountName account) {
            this.binding.setAccount(account);
        }
    }
}
