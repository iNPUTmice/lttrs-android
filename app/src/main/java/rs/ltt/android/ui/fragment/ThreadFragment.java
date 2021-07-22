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

package rs.ltt.android.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.paging.PagedList;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import rs.ltt.android.LttrsNavigationDirections;
import rs.ltt.android.R;
import rs.ltt.android.databinding.FragmentThreadBinding;
import rs.ltt.android.entity.EmailWithBodies;
import rs.ltt.android.entity.ExpandedPosition;
import rs.ltt.android.entity.Seen;
import rs.ltt.android.entity.SubjectWithImportance;
import rs.ltt.android.ui.ItemAnimators;
import rs.ltt.android.ui.ViewIntent;
import rs.ltt.android.ui.activity.ComposeActivity;
import rs.ltt.android.ui.activity.result.CreateDocumentContract;
import rs.ltt.android.ui.adapter.OnAttachmentActionTriggered;
import rs.ltt.android.ui.adapter.OnComposeActionTriggered;
import rs.ltt.android.ui.adapter.OnFlaggedToggled;
import rs.ltt.android.ui.adapter.ThreadAdapter;
import rs.ltt.android.ui.model.ThreadViewModel;
import rs.ltt.android.util.Event;
import rs.ltt.android.util.MediaTypes;
import rs.ltt.jmap.common.entity.Attachment;

public class ThreadFragment extends AbstractLttrsFragment implements OnFlaggedToggled, OnComposeActionTriggered, OnAttachmentActionTriggered {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadFragment.class);

    private FragmentThreadBinding binding;
    private ThreadViewModel threadViewModel;
    private ThreadAdapter threadAdapter;
    private ActivityResultLauncher<Attachment> createDocumentLauncher;

    private ThreadViewModel.MenuConfiguration menuConfiguration = ThreadViewModel.MenuConfiguration.none();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        this.createDocumentLauncher = registerForActivityResult(
                new CreateDocumentContract(),
                uri -> threadViewModel.storeAttachment(uri)
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final ThreadFragmentArgs arguments = ThreadFragmentArgs.fromBundle(requireArguments());
        final String threadId = arguments.getThread();
        final String label = arguments.getLabel();
        final ViewModelProvider viewModelProvider = new ViewModelProvider(
                getViewModelStore(),
                new ThreadViewModel.Factory(
                        requireActivity().getApplication(),
                        getLttrsViewModel().getAccountId(),
                        threadId,
                        label
                )
        );
        getLttrsViewModel().clearActivityTitle();
        threadViewModel = viewModelProvider.get(ThreadViewModel.class);
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_thread, container, false);

        threadViewModel.getSeenEvent().observe(getViewLifecycleOwner(), this::onSeenEvent);
        threadViewModel.getViewIntentEvent().observe(getViewLifecycleOwner(), this::onViewIntentEvent);

        //do we want a custom layout manager that does *NOT* remember scroll position when more
        //than one item is expanded. with variable sized items this might be annoying

        threadAdapter = new ThreadAdapter(threadViewModel.expandedItems);
        threadAdapter.setSubjectWithImportance(SubjectWithImportance.of(
                threadId,
                Strings.emptyToNull(arguments.getSubject()),
                arguments.getImportant()
        ));

        //the default change animation causes UI glitches when expanding or collapsing item
        //for now it's better to just disable it. In the future we may write our own animator
        ItemAnimators.disableChangeAnimation(binding.list.getItemAnimator());

        binding.list.setAdapter(threadAdapter);
        threadViewModel.getEmails().observe(getViewLifecycleOwner(), this::onEmailsChanged);
        threadViewModel.getSubjectWithImportance().observe(getViewLifecycleOwner(), threadAdapter::setSubjectWithImportance);
        threadViewModel.getFlagged().observe(getViewLifecycleOwner(), threadAdapter::setFlagged);
        threadViewModel.getLabels().observe(getViewLifecycleOwner(), threadAdapter::setLabels);
        threadViewModel.getMenuConfiguration().observe(getViewLifecycleOwner(), menuConfiguration -> {
            this.menuConfiguration = menuConfiguration;
            requireActivity().invalidateOptionsMenu();
        });
        threadAdapter.setOnFlaggedToggledListener(this);
        threadAdapter.setOnComposeActionTriggeredListener(this);
        threadAdapter.setOnAttachmentActionTriggered(this);
        threadViewModel.getThreadViewRedirect().observe(getViewLifecycleOwner(), this::onThreadViewRedirect);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        nullReferences();
        super.onDestroyView();
    }

    private void nullReferences() {
        this.binding.list.setAdapter(null);
        this.binding = null;
    }

    private void onSeenEvent(Event<Seen> seenEvent) {
        if (seenEvent.isConsumable()) {
            final Seen seen = seenEvent.consume();
            if (seen.isUnread()) {
                //TODO dismiss notification w/o waiting for round-trip
                getThreadModifier().markRead(
                        ImmutableList.of(threadViewModel.getThreadId())
                );
            }
        }
    }

    private void onViewIntentEvent(final Event<ViewIntent> viewIntentEvent) {
        if (viewIntentEvent.isConsumable()) {
            viewIntentEvent.consume().launch(requireActivity());
        }
    }

    private void onThreadViewRedirect(final Event<String> event) {
        if (event.isConsumable()) {
            final String threadId = event.consume();
            //TODO once draft worker copies over flags and mailboxes we might want to put them in here as well
            getNavController().navigate(LttrsNavigationDirections.actionToThread(
                    threadId,
                    null,
                    null,
                    false
            ));
        }
    }

    private void onEmailsChanged(final PagedList<EmailWithBodies> fullEmails) {
        if (threadViewModel.jumpedToFirstUnread.compareAndSet(false, true)) {
            Futures.addCallback(
                    threadViewModel.expandedPositions,
                    new FutureCallback<List<ExpandedPosition>>() {
                        @Override
                        public void onSuccess(final List<ExpandedPosition> expandedPositions) {
                            threadAdapter.expand(expandedPositions);
                            submitList(fullEmails, () -> {
                                final int pos = expandedPositions.get(0).position;
                                binding.list.scrollToPosition(pos == 0 ? 0 : pos + 1);
                            });
                        }

                        @Override
                        public void onFailure(@NotNull Throwable t) {
                            LOGGER.error("Unable to calculate expanded positions", t);
                            submitList(fullEmails);
                        }
                    },
                    MoreExecutors.directExecutor()
            );
        } else {
            submitList(fullEmails);
        }
    }

    public void submitList(final PagedList<EmailWithBodies> pagedList) {
        submitList(pagedList, null);
    }

    public void submitList(final PagedList<EmailWithBodies> pagedList, final Runnable runnable) {
        configureItemAnimator();
        threadAdapter.submitList(pagedList, runnable);
    }

    private void configureItemAnimator() {
        ItemAnimators.configureItemAnimator(
                this.binding.list,
                this.threadAdapter.isInitialLoad()
        );
    }

    //TODO migrate to contracts
    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        LOGGER.info("on activity result code={}, result={}, intent={}", requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ComposeActivity.REQUEST_EDIT_DRAFT &&
                resultCode == ComposeActivity.RESULT_OK) {
            final UUID uuid = (UUID) data.getSerializableExtra(ComposeActivity.EDITING_TASK_ID_EXTRA);
            final boolean threadDiscarded = data.getBooleanExtra(ComposeActivity.DISCARDED_THREAD_EXTRA, false);
            if (uuid != null) {
                getLttrsViewModel().observeForFailure(uuid);
                threadViewModel.waitForEdit(uuid);
            } else if (threadDiscarded) {
                getNavController().popBackStack();
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_thread, menu);
        menu.findItem(R.id.action_archive).setVisible(menuConfiguration.archive);
        final MenuItem removeLabelItem = menu.findItem(R.id.action_remove_label);
        removeLabelItem.setVisible(menuConfiguration.removeLabel);
        removeLabelItem.setTitle(getString(R.string.remove_label_x, threadViewModel.getLabel()));
        menu.findItem(R.id.action_move_to_inbox).setVisible(menuConfiguration.moveToInbox);
        menu.findItem(R.id.action_move_to_trash).setVisible(menuConfiguration.moveToTrash);
        menu.findItem(R.id.action_mark_important).setVisible(menuConfiguration.markImportant);
        menu.findItem(R.id.action_mark_not_important).setVisible(menuConfiguration.markNotImportant);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        final NavController navController = getNavController();
        final int itemId = menuItem.getItemId();
        if (itemId == R.id.action_mark_unread) {
            getThreadModifier().markUnread(
                    ImmutableList.of(threadViewModel.getThreadId())
            );
            navController.popBackStack();
            return true;
        } else if (itemId == R.id.action_archive) {
            getThreadModifier().archive(
                    ImmutableList.of(threadViewModel.getThreadId())
            );
            navController.popBackStack();
            return true;
        } else if (itemId == R.id.action_remove_label) {
            getThreadModifier().removeFromMailbox(
                    ImmutableList.of(threadViewModel.getThreadId()),
                    threadViewModel.getMailbox()
            );
            navController.popBackStack();
            return true;
        } else if (itemId == R.id.action_move_to_inbox) {
            getThreadModifier().moveToInbox(
                    ImmutableList.of(threadViewModel.getThreadId())
            );
            navController.popBackStack();
            return true;
        } else if (itemId == R.id.action_move_to_trash) {
            getThreadModifier().moveToTrash(
                    ImmutableList.of(threadViewModel.getThreadId())
            );
            navController.popBackStack();
            return true;
        } else if (itemId == R.id.action_change_labels) {
            navController.navigate(LttrsNavigationDirections.actionChangeLabels(
                    new String[]{threadViewModel.getThreadId()}
            ));
            return true;
        } else if (itemId == R.id.action_mark_important) {
            getThreadModifier().markImportant(
                    ImmutableList.of(threadViewModel.getThreadId())
            );
            return true;
        } else if (itemId == R.id.action_mark_not_important) {
            //TODO if label == important (coming from important view); pop back stack
            getThreadModifier().markNotImportant(
                    ImmutableList.of(threadViewModel.getThreadId())
            );
            return true;
        } else {
            return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onFlaggedToggled(String threadId, boolean target) {
        getThreadModifier().toggleFlagged(threadId, target);
    }


    @Override
    public void onEditDraft(String emailId) {
        ComposeActivity.editDraft(
                this,
                getLttrsViewModel().getAccountId(),
                emailId
        );
    }

    @Override
    public void onReplyAll(String emailId) {
        ComposeActivity.replyAll(
                this,
                getLttrsViewModel().getAccountId(),
                emailId
        );
    }

    @Override
    public void onOpenTriggered(final String emailId, final Attachment attachment) {
        threadViewModel.open(emailId, attachment);
    }

    @Override
    public void onActionTriggered(final String emailId, final Attachment attachment) {
        LOGGER.info("launching for {}", MediaTypes.toString(attachment.getMediaType()));
        this.threadViewModel.setAttachmentReference(emailId, attachment.getBlobId());
        this.createDocumentLauncher.launch(attachment);
    }
}
