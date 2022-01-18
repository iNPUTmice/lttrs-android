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
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedList;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.cache.BlobStorage;
import rs.ltt.android.cache.CachedAttachment;
import rs.ltt.android.entity.EmailWithBodies;
import rs.ltt.android.entity.ExpandedPosition;
import rs.ltt.android.entity.MailboxOverwriteEntity;
import rs.ltt.android.entity.MailboxWithRoleAndName;
import rs.ltt.android.entity.Seen;
import rs.ltt.android.entity.SubjectWithImportance;
import rs.ltt.android.entity.ThreadHeader;
import rs.ltt.android.repository.ThreadViewRepository;
import rs.ltt.android.util.CombinedListsLiveData;
import rs.ltt.android.util.Event;
import rs.ltt.android.util.WorkInfoUtils;
import rs.ltt.android.worker.BlobDownloadWorker;
import rs.ltt.android.worker.Failure;
import rs.ltt.android.worker.StoreAttachmentWorker;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.mua.util.LabelUtil;

public class ThreadViewModel extends AbstractAttachmentViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadViewModel.class);

    public final AtomicBoolean jumpedToFirstUnread = new AtomicBoolean(false);
    public final ListenableFuture<List<ExpandedPosition>> expandedPositions;
    public final MutableLiveData<Event<Seen>> seenEvent = new MutableLiveData<>();
    public final HashSet<String> expandedItems = new HashSet<>();
    private final long accountId;
    private final String threadId;
    private final String label;
    private final ThreadViewRepository threadViewRepository;
    private final MediatorLiveData<Event<Collection<Failure>>> decryptionFailures =
            new MediatorLiveData<>();
    private final MediatorLiveData<Event<String>> threadViewRedirect = new MediatorLiveData<>();
    private final MediatorLiveData<SubjectWithImportance> subjectWithImportance =
            new MediatorLiveData<>();
    private final LiveData<PagedList<EmailWithBodies>> emails;
    private final LiveData<Boolean> flagged;
    private final LiveData<List<MailboxWithRoleAndName>> mailboxes;
    private final LiveData<List<MailboxWithRoleAndName>> labels;
    private final LiveData<MenuConfiguration> menuConfiguration;

    private AttachmentReference attachmentReference = null;

    ThreadViewModel(
            @NonNull final Application application,
            final long accountId,
            final String threadId,
            final String label) {
        super(application);
        this.accountId = accountId;
        this.threadId = threadId;
        this.label = label;
        this.threadViewRepository = new ThreadViewRepository(application, accountId);
        final LiveData<ThreadHeader> header = threadViewRepository.getThreadHeader(threadId);
        this.emails = threadViewRepository.getEmails(threadId);
        final LiveData<List<WorkInfo>> decryptionWorkInfo =
                threadViewRepository.getDecryptionWorkInfo(threadId);
        this.decryptionFailures.addSource(decryptionWorkInfo, this::processDecryptionWorkInfo);
        this.mailboxes = threadViewRepository.getMailboxes(threadId);
        final ListenableFuture<Seen> seen = threadViewRepository.getSeen(threadId);
        this.expandedPositions =
                Futures.transform(seen, Seen::getExpandedPositions, MoreExecutors.directExecutor());
        Futures.addCallback(
                seen,
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable Seen seen) {
                        if (seen != null) {
                            seenEvent.postValue(new Event<>(seen));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {}
                },
                MoreExecutors.directExecutor());

        final LiveData<List<MailboxOverwriteEntity>> overwriteEntityLiveData =
                threadViewRepository.getMailboxOverwrites(threadId);

        final CombinedListsLiveData<MailboxOverwriteEntity, MailboxWithRoleAndName> combined =
                new CombinedListsLiveData<>(overwriteEntityLiveData, mailboxes);

        this.labels = Transformations.map(combined, this::getLabels);

        this.menuConfiguration = Transformations.map(combined, this::getMenuConfiguration);

        final LiveData<Boolean> importance = Transformations.map(combined, this::getImportance);

        this.subjectWithImportance.addSource(
                importance, important -> setSubjectWithImportance(header.getValue(), important));
        this.subjectWithImportance.addSource(
                header,
                threadHeader -> setSubjectWithImportance(threadHeader, importance.getValue()));

        this.flagged = Transformations.map(header, h -> h != null && h.showAsFlagged());

        // TODO add LiveData that is true when header != null and display 'Thread not found' or
        //  something in UI
    }

    private MenuConfiguration getMenuConfiguration(
            Pair<List<MailboxOverwriteEntity>, List<MailboxWithRoleAndName>> pair) {
        List<MailboxOverwriteEntity> overwrites = pair.first;
        List<MailboxWithRoleAndName> list = pair.second;
        boolean wasPutInArchiveOverwrite =
                MailboxOverwriteEntity.hasOverwrite(overwrites, Role.ARCHIVE);
        boolean wasPutInTrashOverwrite =
                MailboxOverwriteEntity.hasOverwrite(overwrites, Role.TRASH);
        boolean wasPutInInboxOverwrite =
                MailboxOverwriteEntity.hasOverwrite(overwrites, Role.INBOX);
        final MailboxOverwriteEntity importantOverwrite =
                MailboxOverwriteEntity.find(overwrites, Role.IMPORTANT);

        final boolean removeLabel = MailboxWithRoleAndName.isAnyOfLabel(list, this.label);
        final boolean archive =
                !removeLabel
                        && (MailboxWithRoleAndName.isAnyOfRole(list, Role.INBOX)
                                || wasPutInInboxOverwrite)
                        && !wasPutInArchiveOverwrite
                        && !wasPutInTrashOverwrite;
        final boolean moveToInbox =
                (MailboxWithRoleAndName.isAnyOfRole(list, Role.ARCHIVE)
                                || MailboxWithRoleAndName.isAnyOfRole(list, Role.TRASH)
                                || wasPutInArchiveOverwrite
                                || wasPutInTrashOverwrite)
                        && !wasPutInInboxOverwrite;
        final boolean moveToTrash =
                (MailboxWithRoleAndName.isAnyNotOfRole(list, Role.TRASH) || wasPutInInboxOverwrite)
                        && !wasPutInTrashOverwrite;

        final boolean markedAsImportant =
                importantOverwrite != null
                        ? importantOverwrite.value
                        : MailboxWithRoleAndName.isAnyOfRole(list, Role.IMPORTANT);

        return new MenuConfiguration(
                archive,
                removeLabel,
                moveToInbox,
                moveToTrash,
                !markedAsImportant,
                markedAsImportant);
    }

    private Boolean getImportance(
            final Pair<List<MailboxOverwriteEntity>, List<MailboxWithRoleAndName>> pair) {
        final List<MailboxOverwriteEntity> overwrites = pair.first;
        final List<MailboxWithRoleAndName> list = pair.second;
        final MailboxOverwriteEntity importantOverwrite =
                MailboxOverwriteEntity.find(overwrites, Role.IMPORTANT);
        return importantOverwrite != null
                ? importantOverwrite.value
                : MailboxWithRoleAndName.isAnyOfRole(list, Role.IMPORTANT);
    }

    private List<MailboxWithRoleAndName> getLabels(
            final Pair<List<MailboxOverwriteEntity>, List<MailboxWithRoleAndName>> pair) {
        return combine(pair.first, pair.second).stream()
                .filter(m -> m.getRole() == null || m.getRole() == Role.INBOX)
                .sorted(LabelUtil.COMPARATOR)
                .collect(Collectors.toList());
    }

    private static List<MailboxWithRoleAndName> combine(
            List<MailboxOverwriteEntity> overwrites, List<MailboxWithRoleAndName> mailboxes) {
        final ImmutableList.Builder<MailboxWithRoleAndName> builder = ImmutableList.builder();
        for (final MailboxWithRoleAndName mailbox : mailboxes) {
            final Boolean overwrite = MailboxOverwriteEntity.getOverwrite(overwrites, mailbox);
            if (overwrite == null || Boolean.TRUE.equals(overwrite)) {
                builder.add(mailbox);
            }
        }
        for (final MailboxOverwriteEntity overwrite : overwrites) {
            if (Boolean.TRUE.equals(overwrite.value) && !overwrite.matches(mailboxes)) {
                final Role role = overwrite.role.isEmpty() ? null : Role.valueOf(overwrite.role);
                builder.add(new MailboxWithRoleAndName(role, overwrite.name));
            }
        }
        return builder.build();
    }

    private void setSubjectWithImportance(ThreadHeader header, Boolean important) {
        if (header == null || important == null) {
            return;
        }
        this.subjectWithImportance.postValue(SubjectWithImportance.of(header, important));
    }

    public LiveData<Event<Seen>> getSeenEvent() {
        return this.seenEvent;
    }

    public LiveData<Event<String>> getThreadViewRedirect() {
        return this.threadViewRedirect;
    }

    public LiveData<PagedList<EmailWithBodies>> getEmails() {
        return emails;
    }

    public LiveData<SubjectWithImportance> getSubjectWithImportance() {
        return this.subjectWithImportance;
    }

    public LiveData<Boolean> getFlagged() {
        return this.flagged;
    }

    public LiveData<MenuConfiguration> getMenuConfiguration() {
        return menuConfiguration;
    }

    public String getLabel() {
        return this.label;
    }

    public LiveData<List<MailboxWithRoleAndName>> getLabels() {
        return this.labels;
    }

    public String getThreadId() {
        return this.threadId;
    }

    public MailboxWithRoleAndName getMailbox() {
        final List<MailboxWithRoleAndName> mailboxes = this.mailboxes.getValue();
        final MailboxWithRoleAndName mailbox =
                mailboxes == null
                        ? null
                        : MailboxWithRoleAndName.findByLabel(mailboxes, this.label);
        if (mailbox == null) {
            throw new IllegalStateException("No mailbox found with the label " + this.label);
        }
        return mailbox;
    }

    public void waitForEdit(UUID uuid) {
        final WorkManager workManager = WorkManager.getInstance(getApplication());
        final LiveData<WorkInfo> liveData = workManager.getWorkInfoByIdLiveData(uuid);
        threadViewRedirect.addSource(
                liveData,
                workInfo -> {
                    if (workInfo.getState().isFinished()) {
                        threadViewRedirect.removeSource(liveData);
                    }
                    if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        final Data data = workInfo.getOutputData();
                        final String threadId = data.getString("threadId");
                        if (threadId != null && !ThreadViewModel.this.threadId.equals(threadId)) {
                            LOGGER.info("redirecting to thread {}", threadId);
                            threadViewRedirect.postValue(new Event<>(threadId));
                        }
                    }
                });
    }

    public void setAttachmentReference(final String emailId, final String blobId) {
        this.attachmentReference = new AttachmentReference(emailId, blobId);
    }

    public void storeAttachment(final Uri uri) {
        final AttachmentReference attachment = this.attachmentReference;
        if (attachment == null) {
            throw new IllegalStateException("AttachmentReference has not been set");
        }
        this.attachmentReference = null;
        final ListenableFuture<CachedAttachment> future =
                BlobStorage.getIfCached(getApplication(), accountId, attachment.blobId);
        Futures.addCallback(
                future,
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(final CachedAttachment cachedAttachment) {
                        storeAttachment(cachedAttachment, uri);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable throwable) {
                        if (throwable instanceof CachedAttachment.InvalidCacheException) {
                            downloadAndStoreAttachment(attachment, uri);
                        } else {
                            // TODO display error message?
                        }
                    }
                },
                MoreExecutors.directExecutor());
    }

    private void storeAttachment(final CachedAttachment cachedAttachment, final Uri uri) {
        final WorkManager workManager = WorkManager.getInstance(getApplication());
        final OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(StoreAttachmentWorker.class)
                        .setInputData(StoreAttachmentWorker.data(cachedAttachment.getFile(), uri))
                        .build();
        workManager.enqueue(workRequest);
    }

    private void downloadAndStoreAttachment(
            final AttachmentReference attachmentReference, final Uri uri) {
        final WorkManager workManager = WorkManager.getInstance(getApplication());
        final OneTimeWorkRequest downloadWorkRequest =
                new OneTimeWorkRequest.Builder(BlobDownloadWorker.class)
                        .setInputData(
                                BlobDownloadWorker.data(
                                        accountId,
                                        attachmentReference.emailId,
                                        attachmentReference.blobId))
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build();
        final OneTimeWorkRequest storeWorkRequest =
                new OneTimeWorkRequest.Builder(StoreAttachmentWorker.class)
                        .setInputData(StoreAttachmentWorker.data(uri))
                        .build();
        workManager
                .beginUniqueWork(
                        BlobDownloadWorker.uniqueName(),
                        ExistingWorkPolicy.APPEND_OR_REPLACE,
                        downloadWorkRequest)
                .then(storeWorkRequest)
                .enqueue();
    }

    @Override
    protected long getAccountIdOrThrow() {
        return this.accountId;
    }

    public LiveData<Event<Collection<Failure>>> getDecryptionFailures() {
        return this.decryptionFailures;
    }

    public void decryptEmail(final String emailId) {
        final LiveData<List<WorkInfo>> decryptionWorkInfo =
                threadViewRepository.decryptEmail(emailId);
        this.decryptionFailures.addSource(decryptionWorkInfo, this::processDecryptionWorkInfo);
    }

    private void processDecryptionWorkInfo(final List<WorkInfo> workInfo) {
        if (WorkInfoUtils.finished(workInfo)) {
            final Collection<WorkInfo> failed = WorkInfoUtils.failed(workInfo);
            this.decryptionFailures.postValue(new Event<>(Failure.of(failed)));
        }
    }

    public static class MenuConfiguration {
        public final boolean archive;
        public final boolean removeLabel;
        public final boolean moveToInbox;
        public final boolean moveToTrash;
        public final boolean markImportant;
        public final boolean markNotImportant;

        MenuConfiguration(
                boolean archive,
                boolean removeLabel,
                boolean moveToInbox,
                boolean moveToTrash,
                boolean markImportant,
                boolean markNotImportant) {
            this.archive = archive;
            this.removeLabel = removeLabel;
            this.moveToInbox = moveToInbox;
            this.moveToTrash = moveToTrash;
            this.markImportant = markImportant;
            this.markNotImportant = markNotImportant;
        }

        public static MenuConfiguration none() {
            return new MenuConfiguration(false, false, false, false, false, false);
        }
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final Application application;
        private final long accountId;
        private final String threadId;
        private final String label;

        public Factory(
                final Application application,
                final long accountId,
                final String threadId,
                final String label) {
            this.application = application;
            this.accountId = accountId;
            this.threadId = threadId;
            this.label = label;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return Objects.requireNonNull(
                    modelClass.cast(new ThreadViewModel(application, accountId, threadId, label)));
        }
    }

    private static class AttachmentReference {
        public final String emailId;
        public final String blobId;

        private AttachmentReference(String emailId, String blobId) {
            this.emailId = emailId;
            this.blobId = blobId;
        }
    }
}
