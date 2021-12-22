package rs.ltt.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import java.util.UUID;
import rs.ltt.android.R;
import rs.ltt.android.databinding.FragmentReassignRoleBinding;
import rs.ltt.android.ui.model.ReassignRoleViewModel;
import rs.ltt.android.util.Event;
import rs.ltt.jmap.common.entity.Role;

public class ReassignRoleFragment extends AbstractLttrsFragment {

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ReassignRoleFragmentArgs arguments =
                ReassignRoleFragmentArgs.fromBundle(getArguments());

        final ViewModelProvider viewModelProvider =
                new ViewModelProvider(
                        getViewModelStore(),
                        new ReassignRoleViewModel.Factory(
                                requireActivity().getApplication(),
                                getLttrsViewModel().getAccountId(),
                                arguments.getMailbox(),
                                Role.valueOf(arguments.getRole())));
        final ReassignRoleViewModel viewModel = viewModelProvider.get(ReassignRoleViewModel.class);
        final FragmentReassignRoleBinding binding =
                DataBindingUtil.inflate(
                        inflater, R.layout.fragment_reassign_role, container, false);
        viewModel
                .isReassignment()
                .observe(
                        getViewLifecycleOwner(),
                        reassign ->
                                getLttrsViewModel()
                                        .setActivityTitle(
                                                reassign != null && reassign
                                                        ? R.string.reassign_role
                                                        : R.string.assign_role));
        binding.setModel(viewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.cancel.setOnClickListener(this::onCancel);
        viewModel
                .getWorkerDispatchedEvent()
                .observe(getViewLifecycleOwner(), this::onWorkerDispatchedEvent);
        return binding.getRoot();
    }

    private void onWorkerDispatchedEvent(final Event<UUID> event) {
        if (event.isConsumable()) {
            final UUID id = event.consume();
            getLttrsViewModel().observeForFailure(id);
            getNavController().navigateUp();
        }
    }

    private void onCancel(View view) {
        getNavController().navigateUp();
    }
}
