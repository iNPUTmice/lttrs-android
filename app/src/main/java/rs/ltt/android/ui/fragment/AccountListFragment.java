package rs.ltt.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import java.util.List;
import rs.ltt.android.R;
import rs.ltt.android.databinding.FragmentAccountListBinding;
import rs.ltt.android.entity.AccountName;
import rs.ltt.android.ui.activity.SetupActivity;
import rs.ltt.android.ui.adapter.AccountAdapter;
import rs.ltt.android.ui.model.AccountListViewModel;

public class AccountListFragment extends AbstractAccountManagerFragment {

    private FragmentAccountListBinding binding;
    private AccountAdapter accountAdapter;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        this.binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_account_list, container, false);

        final ViewModelProvider viewModelProvider =
                new ViewModelProvider(getViewModelStore(), getDefaultViewModelProviderFactory());
        final AccountListViewModel accountListViewModel =
                viewModelProvider.get(AccountListViewModel.class);

        this.accountAdapter = new AccountAdapter();
        this.binding.accountList.setAdapter(this.accountAdapter);

        this.accountAdapter.setOnAccountSelected(this::navigateToAccount);

        accountListViewModel
                .getAccounts()
                .observe(getViewLifecycleOwner(), this::onAccountsUpdated);

        this.binding.addNewAccount.setOnClickListener(v -> SetupActivity.launch(requireActivity()));

        return this.binding.getRoot();
    }

    private void navigateToAccount(final long id) {
        getNavController().navigate(AccountListFragmentDirections.actionAccountListToAccount(id));
    }

    @Override
    public void onDestroyView() {
        nullReferences();
        super.onDestroyView();
    }

    private void nullReferences() {
        this.binding.accountList.setAdapter(null);
        this.binding = null;
    }

    private void onAccountsUpdated(final List<AccountName> accounts) {
        this.accountAdapter.submitList(accounts);
    }
}
