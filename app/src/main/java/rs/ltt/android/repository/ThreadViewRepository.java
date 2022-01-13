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
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.entity.EmailWithBodies;
import rs.ltt.android.entity.EmailWithEncryptionStatus;
import rs.ltt.android.entity.EncryptionStatus;
import rs.ltt.android.entity.ExpandedPosition;
import rs.ltt.android.entity.KeywordOverwriteEntity;
import rs.ltt.android.entity.MailboxOverwriteEntity;
import rs.ltt.android.entity.MailboxWithRoleAndName;
import rs.ltt.android.entity.Seen;
import rs.ltt.android.entity.ThreadHeader;
import rs.ltt.android.worker.DecryptionWorker;

public class ThreadViewRepository extends AbstractRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadViewRepository.class);

    public ThreadViewRepository(final Application application, final long accountId) {
        super(application, accountId);
    }

    public LiveData<PagedList<EmailWithBodies>> getEmails(String threadId) {
        return new LivePagedListBuilder<>(database.threadAndEmailDao().getEmails(threadId), 30)
                .build();
    }

    public LiveData<ThreadHeader> getThreadHeader(String threadId) {
        return database.threadAndEmailDao().getThreadHeader(threadId);
    }

    public LiveData<List<MailboxWithRoleAndName>> getMailboxes(String threadId) {
        return database.mailboxDao().getMailboxesForThreadLiveData(threadId);
    }

    public LiveData<List<MailboxOverwriteEntity>> getMailboxOverwrites(String threadId) {
        return database.overwriteDao().getMailboxOverwrites(threadId);
    }

    public LiveData<List<WorkInfo>> getDecryptionWorkInfo(final String threadId) {
        final LiveData<List<EmailWithEncryptionStatus>> encryptedEmails =
                database.threadAndEmailDao()
                        .getEmailsWithEncryptionStatus(threadId, EncryptionStatus.ENCRYPTED);
        final HashSet<UUID> enqueuedWork = new HashSet<>();
        return Transformations.switchMap(
                encryptedEmails, emails -> enqueueDecryptionWorkers(emails, enqueuedWork));
    }

    public LiveData<List<WorkInfo>> decryptEmail(final String emailId) {
        final WorkManager workManager = WorkManager.getInstance(application);
        final OneTimeWorkRequest oneTimeWorkRequest =
                new OneTimeWorkRequest.Builder(DecryptionWorker.class)
                        .setInputData(DecryptionWorker.data(accountId, emailId))
                        .build();
        workManager.enqueueUniqueWork(
                DecryptionWorker.uniqueName(accountId, emailId),
                ExistingWorkPolicy.KEEP,
                oneTimeWorkRequest);
        return workManager.getWorkInfosLiveData(
                WorkQuery.Builder.fromIds(ImmutableList.of(oneTimeWorkRequest.getId())).build());
    }

    private LiveData<List<WorkInfo>> enqueueDecryptionWorkers(
            final List<EmailWithEncryptionStatus> emails, final HashSet<UUID> enqueuedWork) {
        LOGGER.info("Enqueue decryption jobs for {}", emails);
        final WorkManager workManager = WorkManager.getInstance(application);
        for (final EmailWithEncryptionStatus email : emails) {
            final OneTimeWorkRequest oneTimeWorkRequest =
                    new OneTimeWorkRequest.Builder(DecryptionWorker.class)
                            .setInputData(DecryptionWorker.data(accountId, email.id))
                            .build();
            enqueuedWork.add(oneTimeWorkRequest.getId());
            workManager.enqueueUniqueWork(
                    DecryptionWorker.uniqueName(accountId, email.id),
                    ExistingWorkPolicy.KEEP,
                    oneTimeWorkRequest);
        }
        if (enqueuedWork.isEmpty()) {
            return new MutableLiveData<>();
        }
        return workManager.getWorkInfosLiveData(
                WorkQuery.Builder.fromIds(ImmutableList.copyOf(enqueuedWork)).build());
    }

    public ListenableFuture<Seen> getSeen(String threadId) {
        ListenableFuture<KeywordOverwriteEntity> overwriteFuture =
                database.overwriteDao().getKeywordOverwrite(threadId);
        return Futures.transformAsync(
                overwriteFuture,
                overwrite -> {
                    if (overwrite != null) {
                        if (overwrite.value) {
                            return Seen.of(
                                    true, database.threadAndEmailDao().getMaxPosition(threadId));
                        } else {
                            return Seen.of(
                                    false, database.threadAndEmailDao().getAllPositions(threadId));
                        }
                    } else {
                        ListenableFuture<List<ExpandedPosition>> unseenFuture =
                                database.threadAndEmailDao().getUnseenPositions(threadId);
                        return Futures.transformAsync(
                                unseenFuture,
                                unseen -> {
                                    if (unseen == null || unseen.size() == 0) {
                                        return Seen.of(
                                                true,
                                                database.threadAndEmailDao()
                                                        .getMaxPosition(threadId));
                                    } else {
                                        return Seen.of(false, Futures.immediateFuture(unseen));
                                    }
                                },
                                MoreExecutors.directExecutor());
                    }
                },
                MoreExecutors.directExecutor());
    }
}
