package rs.ltt.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import rs.ltt.android.R;
import rs.ltt.android.databinding.FragmentAccountBinding;
import rs.ltt.android.ui.activity.AccountManagerActivity;
import rs.ltt.android.ui.activity.LttrsActivity;
import rs.ltt.android.ui.model.AccountViewModel;
import rs.ltt.android.util.Event;

public class AccountFragment extends AbstractAccountManagerFragment {

    private AccountViewModel accountViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final AccountFragmentArgs arguments = AccountFragmentArgs.fromBundle(requireArguments());
        final long accountId = arguments.getId();

        final FragmentAccountBinding binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_account, container, false);

        final ViewModelProvider viewModelProvider =
                new ViewModelProvider(
                        getViewModelStore(),
                        new AccountViewModel.Factory(
                                requireActivity().getApplication(), accountId));
        this.accountViewModel = viewModelProvider.get(AccountViewModel.class);

        this.accountViewModel
                .getAccountName()
                .observe(getViewLifecycleOwner(), binding::setAccount);
        this.accountViewModel
                .getOnFinishEvent()
                .observe(getViewLifecycleOwner(), this::onFinishEvent);
        this.accountViewModel
                .isEnabled()
                .observe(getViewLifecycleOwner(), e -> binding.setEnabled(Boolean.TRUE.equals(e)));

        binding.remove.setOnClickListener(this::onRemoveAccount);
        binding.identities.setOnClickListener(this::onIdentities);
        binding.labels.setOnClickListener(this::onLabels);
        binding.vacationResponse.setOnClickListener(this::onVacationResponse);
        binding.e2ee.setOnClickListener(this::onE2ee);

        return binding.getRoot();
    }

    private void onFinishEvent(final Event<Void> event) {
        if (event.isConsumable()) {
            event.consume();
            AccountManagerActivity.relaunch(requireActivity());
        }
    }

    @Override
    public void onCreateOptionsMenu(
            @NonNull final Menu menu, @NonNull final MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_account, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        final int itemId = menuItem.getItemId();
        if (itemId == R.id.launch) {
            LttrsActivity.launch(getActivity(), accountViewModel.getAccountId());
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void onE2ee(final View view) {
        final long id = accountViewModel.getAccountId();
        getNavController()
                .navigate(AccountFragmentDirections.actionAccountToEncryptionSettings(id));
    }

    private void onVacationResponse(final View view) {}

    private void onLabels(final View view) {}

    private void onIdentities(final View view) {}

    private void onRemoveAccount(final View view) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.remove_account_dialog_title)
                .setMessage(R.string.remove_account_dialog_message)
                .setPositiveButton(
                        R.string.remove_account,
                        (dialog, which) -> accountViewModel.removeAccount())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
