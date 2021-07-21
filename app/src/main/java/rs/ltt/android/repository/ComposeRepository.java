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

package rs.ltt.android.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import rs.ltt.android.entity.EditableEmail;
import rs.ltt.android.entity.IdentityWithNameAndEmail;
import rs.ltt.android.ui.model.ComposeViewModel;
import rs.ltt.android.worker.AbstractMuaWorker;
import rs.ltt.android.worker.DiscardDraftWorker;
import rs.ltt.android.worker.SaveDraftWorker;
import rs.ltt.android.worker.SendEmailWorker;
import rs.ltt.android.worker.SubmitEmailWorker;
import rs.ltt.jmap.common.entity.IdentifiableIdentity;
import rs.ltt.jmap.common.entity.Keyword;
import rs.ltt.jmap.common.entity.Role;

public class ComposeRepository extends AbstractRepository {

    public ComposeRepository(final Application application, final long accountId) {
        super(application, accountId);
    }

    public LiveData<List<IdentityWithNameAndEmail>> getIdentities() {
        return database.identityDao().getIdentitiesLiveData(accountId);
    }

    public ListenableFuture<EditableEmail> getEditableEmail(final String id) {
        return database.threadAndEmailDao().getEditableEmail(accountId, id);
    }

    public UUID sendEmail(IdentifiableIdentity identity, ComposeViewModel.Draft draft, final Collection<String> inReplyTo, EditableEmail discard) {
        final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SendEmailWorker.class)
                .setConstraints(CONNECTED_CONSTRAINT)
                .setInputData(SendEmailWorker.data(
                        accountId,
                        identity.getId(),
                        inReplyTo,
                        draft.getTo(),
                        draft.getCc(),
                        draft.getSubject(),
                        draft.getBody(),
                        draft.getAttachments()
                ))
                .build();
        final WorkManager workManager = WorkManager.getInstance(application);
        final WorkContinuation continuation = workManager.beginUniqueWork(
                AbstractMuaWorker.uniqueName(accountId),
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                workRequest);
        if (discard != null) {
            final OneTimeWorkRequest discardPreviousDraft = new OneTimeWorkRequest.Builder(DiscardDraftWorker.class)
                    .setConstraints(CONNECTED_CONSTRAINT)
                    .setInputData(DiscardDraftWorker.data(accountId, discard.id))
                    .build();
            continuation.then(discardPreviousDraft).enqueue();
        } else {
            continuation.enqueue();
        }
        workManager.enqueue(workRequest);
        return workRequest.getId();
    }

    public UUID submitEmail(IdentityWithNameAndEmail identity, EditableEmail editableEmail) {
        final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SubmitEmailWorker.class)
                .setConstraints(CONNECTED_CONSTRAINT)
                .setInputData(SubmitEmailWorker.data(
                        accountId,
                        identity.getId(),
                        editableEmail.id
                ))
                .build();
        final WorkManager workManager = WorkManager.getInstance(application);
        workManager.enqueue(workRequest);
        return workRequest.getId();
    }

    public UUID saveDraft(final IdentifiableIdentity identity,
                          final ComposeViewModel.Draft draft,
                          final Collection<String> inReplyTo,
                          final EditableEmail discard) {
        final OneTimeWorkRequest saveDraftRequest = new OneTimeWorkRequest.Builder(SaveDraftWorker.class)
                .setConstraints(CONNECTED_CONSTRAINT)
                .setInputData(SendEmailWorker.data(
                        accountId,
                        identity.getId(),
                        inReplyTo,
                        draft.getTo(),
                        draft.getCc(),
                        draft.getSubject(),
                        draft.getBody(),
                        draft.getAttachments()
                ))
                .build();
        final WorkManager workManager = WorkManager.getInstance(application);
        final WorkContinuation continuation = workManager.beginUniqueWork(
                AbstractMuaWorker.uniqueName(accountId),
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                saveDraftRequest
        );
        if (discard != null) {
            final OneTimeWorkRequest discardPreviousDraft = new OneTimeWorkRequest.Builder(DiscardDraftWorker.class)
                    .setConstraints(CONNECTED_CONSTRAINT)
                    .setInputData(DiscardDraftWorker.data(accountId, discard.id))
                    .build();
            continuation.then(discardPreviousDraft).enqueue();
        } else {
            continuation.enqueue();
        }
        return saveDraftRequest.getId();
    }

    public boolean discard(EditableEmail editableEmail) {
        final OneTimeWorkRequest discardDraft = new OneTimeWorkRequest.Builder(DiscardDraftWorker.class)
                .setConstraints(CONNECTED_CONSTRAINT)
                .setInputData(DiscardDraftWorker.data(accountId, editableEmail.id))
                .build();
        final WorkManager workManager = WorkManager.getInstance(application);
        final boolean isOnlyEmailInThread = editableEmail.isOnlyEmailInThread();
        if (isOnlyEmailInThread) {
            insertQueryItemOverwrite(editableEmail.threadId);
        }
        workManager.enqueue(discardDraft);
        return isOnlyEmailInThread;
    }

    private void insertQueryItemOverwrite(final String threadId) {
        IO_EXECUTOR.execute(() -> {
            insertQueryItemOverwrite(threadId, Role.DRAFTS);
            insertQueryItemOverwrite(threadId, Keyword.DRAFT);
        });
    }

}
