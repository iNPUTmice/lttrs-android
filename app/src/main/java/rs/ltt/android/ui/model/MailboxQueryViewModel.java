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

package rs.ltt.android.ui.model;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import rs.ltt.android.entity.MailboxOverviewItem;
import rs.ltt.android.entity.QueryInfo;
import rs.ltt.android.ui.EmptyMailboxAction;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mua.util.StandardQueries;

public class MailboxQueryViewModel extends AbstractQueryViewModel {

    private final LiveData<MailboxOverviewItem> mailbox;
    private final LiveData<EmptyMailboxAction> emptyMailboxAction;
    private final String mailboxId;

    private final LiveData<EmailQuery> emailQueryLiveData;

    // TODO refactor out MainMailboxQueryViewModel to not have to deal with mailboxId==null special
    // treatment
    MailboxQueryViewModel(
            final Application application, final long accountId, final String mailboxId) {
        super(application, accountId);
        this.mailboxId = mailboxId;
        this.mailbox = this.queryRepository.getMailboxOverviewItem(mailboxId);
        this.emailQueryLiveData =
                Transformations.map(
                        mailbox,
                        mailbox -> {
                            if (mailbox == null) {
                                return EmailQuery.unfiltered(true);
                            } else {
                                return StandardQueries.mailbox(mailbox);
                            }
                        });
        this.emptyMailboxAction =
                Transformations.map(
                        this.mailbox,
                        mailbox -> {
                            if (EmptyMailboxAction.emptyWorthy(mailbox)) {
                                return new EmptyMailboxAction(
                                        mailbox.getRole(), mailbox.totalEmails);
                            } else {
                                return null;
                            }
                        });
        init();
    }

    public LiveData<MailboxOverviewItem> getMailbox() {
        return mailbox;
    }

    @Override
    public LiveData<EmptyMailboxAction> getEmptyMailboxAction() {
        return this.emptyMailboxAction;
    }

    @Override
    protected LiveData<EmailQuery> getQuery() {
        return emailQueryLiveData;
    }

    @Override
    public QueryInfo getQueryInfo() {
        if (mailboxId == null) {
            return new QueryInfo(queryRepository.getAccountId(), QueryInfo.Type.MAIN, null);
        } else {
            return new QueryInfo(queryRepository.getAccountId(), QueryInfo.Type.MAILBOX, mailboxId);
        }
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final Application application;
        private final long accountId;
        private final String id;

        public Factory(Application application, final long accountId, String id) {
            this.application = application;
            this.accountId = accountId;
            this.id = id;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return modelClass.cast(new MailboxQueryViewModel(application, accountId, id));
        }
    }
}
