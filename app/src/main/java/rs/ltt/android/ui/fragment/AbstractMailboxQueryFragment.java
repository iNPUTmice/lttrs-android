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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import rs.ltt.android.entity.MailboxOverviewItem;
import rs.ltt.android.entity.ThreadOverviewItem;
import rs.ltt.android.ui.ActionModeMenuConfiguration;
import rs.ltt.android.ui.QueryItemTouchHelper;
import rs.ltt.android.ui.model.AbstractQueryViewModel;
import rs.ltt.android.ui.model.MailboxQueryViewModel;
import rs.ltt.jmap.common.entity.Role;

public abstract class AbstractMailboxQueryFragment extends AbstractQueryFragment {

    MailboxQueryViewModel mailboxQueryViewModel;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewModelProvider viewModelProvider =
                new ViewModelProvider(
                        getViewModelStore(),
                        new MailboxQueryViewModel.Factory(
                                requireActivity().getApplication(),
                                getLttrsViewModel().getAccountId(),
                                getMailboxId()));
        this.mailboxQueryViewModel = viewModelProvider.get(MailboxQueryViewModel.class);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected AbstractQueryViewModel getQueryViewModel() {
        return this.mailboxQueryViewModel;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mailboxQueryViewModel
                .getMailbox()
                .observe(
                        getViewLifecycleOwner(),
                        mailboxOverviewItem -> {
                            if (mailboxOverviewItem == null) {
                                return;
                            }
                            onLabelOpened(mailboxOverviewItem);
                        });
    }

    @Override
    protected QueryItemTouchHelper.Swipable onQueryItemSwipe(ThreadOverviewItem item) {
        final MailboxOverviewItem mailbox =
                mailboxQueryViewModel != null
                        ? mailboxQueryViewModel.getMailbox().getValue()
                        : null;
        if (mailbox == null) {
            return QueryItemTouchHelper.Swipable.NO;
        } else if (mailbox.role == Role.INBOX) {
            return QueryItemTouchHelper.Swipable.ARCHIVE;
        } else if (mailbox.role == Role.FLAGGED) {
            return QueryItemTouchHelper.Swipable.REMOVE_FLAGGED;
        } else if (mailbox.role == null || mailbox.role == Role.IMPORTANT) {
            return QueryItemTouchHelper.Swipable.REMOVE_LABEL;
        } else {
            return QueryItemTouchHelper.Swipable.NO;
        }
    }

    @Override
    protected boolean isQueryItemRemovedAfterSwipe() {
        return true;
    }

    @Override
    protected boolean showComposeButton() {
        return true;
    }

    protected abstract String getMailboxId();

    @Override
    protected ActionModeMenuConfiguration.QueryType getQueryType() {
        final MailboxOverviewItem mailbox =
                mailboxQueryViewModel != null
                        ? mailboxQueryViewModel.getMailbox().getValue()
                        : null;
        if (mailbox == null) {
            return ActionModeMenuConfiguration.QueryType.SPECIAL;
        } else if (mailbox.role == Role.INBOX) {
            return ActionModeMenuConfiguration.QueryType.INBOX;
        } else if (mailbox.role == Role.ARCHIVE) {
            return ActionModeMenuConfiguration.QueryType.ARCHIVE;
        } else if (mailbox.role == Role.FLAGGED) {
            return ActionModeMenuConfiguration.QueryType.FLAGGED;
        } else if (mailbox.role == Role.TRASH) {
            return ActionModeMenuConfiguration.QueryType.TRASH;
        } else if (mailbox.role == null || mailbox.role == Role.IMPORTANT) {
            return ActionModeMenuConfiguration.QueryType.IMPORTANT;
        } else {
            return ActionModeMenuConfiguration.QueryType.SPECIAL;
        }
    }
}
