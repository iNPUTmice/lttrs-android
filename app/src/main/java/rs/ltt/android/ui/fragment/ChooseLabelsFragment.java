package rs.ltt.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;
import java.util.UUID;
import rs.ltt.android.R;
import rs.ltt.android.databinding.DialogViewNewLabelBinding;
import rs.ltt.android.databinding.FragmentLabelAsBinding;
import rs.ltt.android.entity.SelectableMailbox;
import rs.ltt.android.ui.adapter.ChooseLabelsAdapter;
import rs.ltt.android.ui.model.ChooseLabelsViewModel;
import rs.ltt.android.util.CharSequences;

public class ChooseLabelsFragment extends AbstractLttrsFragment
        implements ChooseLabelsAdapter.OnSelectableMailboxClickListener {

    private FragmentLabelAsBinding binding;

    private ChooseLabelsAdapter chooseLabelsAdapter;
    private ChooseLabelsViewModel viewModel;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        getLttrsViewModel().setActivityTitle(R.string.label_as);
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_label_as, container, false);

        this.chooseLabelsAdapter = new ChooseLabelsAdapter();
        this.chooseLabelsAdapter.setOnSelectableMailboxClickListener(this);

        binding.labelList.setAdapter(this.chooseLabelsAdapter);

        binding.cancel.setOnClickListener(this::onCancel);
        binding.confirm.setOnClickListener(this::onConfirm);
        binding.add.setOnClickListener(this::onAdd);

        final ChooseLabelsFragmentArgs arguments =
                ChooseLabelsFragmentArgs.fromBundle(requireArguments());

        final ViewModelProvider viewModelProvider =
                new ViewModelProvider(
                        getViewModelStore(),
                        new ChooseLabelsViewModel.Factory(
                                requireActivity().getApplication(),
                                getLttrsViewModel().getAccountId(),
                                arguments.getThreads()));
        this.viewModel = viewModelProvider.get(ChooseLabelsViewModel.class);
        this.viewModel
                .getSelectableMailboxesLiveData()
                .observe(getViewLifecycleOwner(), this.chooseLabelsAdapter::submitList);
        return binding.getRoot();
    }

    private void onAdd(final View view) {
        final MaterialAlertDialogBuilder materialAlertDialogBuilder =
                new MaterialAlertDialogBuilder(requireContext());
        materialAlertDialogBuilder.setTitle(R.string.new_label);
        materialAlertDialogBuilder.setNegativeButton(R.string.cancel, null);
        final DialogViewNewLabelBinding dialogViewNewLabelBinding =
                DialogViewNewLabelBinding.inflate(getLayoutInflater());
        materialAlertDialogBuilder.setView(dialogViewNewLabelBinding.getRoot());
        materialAlertDialogBuilder.setPositiveButton(R.string.create, null);
        final AlertDialog dialog = materialAlertDialogBuilder.create();
        dialog.setOnShowListener(
                d -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setOnClickListener(
                                    v -> {
                                        final String name =
                                                CharSequences.nullToEmpty(
                                                                dialogViewNewLabelBinding.name
                                                                        .getText())
                                                        .trim();
                                        try {
                                            viewModel.createLabel(name);
                                            dialog.dismiss();
                                        } catch (final IllegalArgumentException e) {
                                            dialogViewNewLabelBinding.inputLayout.setError(
                                                    e.getMessage());
                                        }
                                    });
                });
        dialog.show();
    }

    private void onConfirm(final View view) {
        final List<UUID> workRequests = this.viewModel.applyChanges();
        getLttrsViewModel().observeForFailure(workRequests);
        final NavController navController = getNavController();
        if (navController.getCurrentDestination().getId() == R.id.label_as) {
            getNavController().navigateUp();
        }
    }

    private void onCancel(final View view) {
        getNavController().navigateUp();
    }

    @Override
    public void onSelectableMailboxClick(final SelectableMailbox mailbox) {
        this.viewModel.setSelectionOverwrite(mailbox, !mailbox.isSelected());
    }
}
