package rs.ltt.android.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import rs.ltt.android.entity.MailboxOverwriteEntity;
import rs.ltt.android.entity.MailboxWithRoleAndName;
import rs.ltt.android.worker.ModifyLabelsWorker;
import rs.ltt.android.worker.SetMailboxRoleWorker;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRole;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRoleAndName;
import rs.ltt.jmap.common.entity.Role;

public class MailboxRepository extends AbstractRepository {

    public MailboxRepository(Application application, long accountId) {
        super(application, accountId);
    }

    public LiveData<MailboxWithRoleAndName> getMailbox(final String id) {
        return database.mailboxDao().getMailboxLiveData(id);
    }

    public LiveData<List<MailboxWithRoleAndName>> getLabels() {
        return database.mailboxDao().getLabels();
    }

    public LiveData<List<String>> getMailboxIdsForThreadsLiveData(final String[] threadIds) {
        return database.mailboxDao().getMailboxIdsForThreadsLiveData(threadIds);
    }

    public LiveData<List<MailboxOverwriteEntity>> getMailboxOverwrites(String[] threadIds) {
        return database.overwriteDao().getMailboxOverwrites(threadIds);
    }

    public UUID setRole(final IdentifiableMailboxWithRole mailbox, final Role role) {
        final OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(SetMailboxRoleWorker.class)
                        .setConstraints(CONNECTED_CONSTRAINT)
                        .setInputData(SetMailboxRoleWorker.data(accountId, mailbox.getId(), role))
                        .build();
        final WorkManager workManager = WorkManager.getInstance(application);
        workManager.enqueueUniqueWork(
                SetMailboxRoleWorker.uniqueName(accountId),
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                workRequest);
        return workRequest.getId();
    }

    public List<UUID> modifyLabels(
            final Collection<String> threadIds,
            final List<IdentifiableMailboxWithRoleAndName> add,
            final List<IdentifiableMailboxWithRoleAndName> remove) {
        if (add.size() == 0 && remove.size() == 0) {
            return Collections.emptyList();
        }
        final List<OneTimeWorkRequest> workRequests =
                threadIds.stream()
                        .map(
                                threadId ->
                                        new OneTimeWorkRequest.Builder(ModifyLabelsWorker.class)
                                                .setConstraints(CONNECTED_CONSTRAINT)
                                                .setInputData(
                                                        ModifyLabelsWorker.data(
                                                                accountId, threadId, add, remove))
                                                .build())
                        .collect(Collectors.toList());
        IO_EXECUTOR.execute(
                () -> {
                    if (add.size() > 0) {
                        insertQueryItemOverwrite(threadIds, Role.TRASH);
                        database.overwriteDao()
                                .insertMailboxOverwrites(
                                        MailboxOverwriteEntity.of(threadIds, Role.TRASH, false));
                    }
                    for (final IdentifiableMailboxWithRoleAndName mailbox : add) {
                        if (Objects.nonNull(mailbox.getId())) {
                            deleteQueryItemOverwrite(threadIds, mailbox);
                        }
                        if (mailbox.getRole() == Role.INBOX) {
                            database.overwriteDao()
                                    .insertMailboxOverwrites(
                                            MailboxOverwriteEntity.of(threadIds, Role.INBOX, true));
                            database.overwriteDao()
                                    .insertMailboxOverwrites(
                                            MailboxOverwriteEntity.of(
                                                    threadIds, Role.ARCHIVE, false));
                            insertQueryItemOverwrite(threadIds, Role.ARCHIVE);
                        }
                    }
                    for (final IdentifiableMailboxWithRoleAndName mailbox : remove) {
                        insertQueryItemOverwrite(threadIds, mailbox);
                        if (mailbox.getRole() == Role.INBOX) {
                            database.overwriteDao()
                                    .insertMailboxOverwrites(
                                            MailboxOverwriteEntity.of(
                                                    threadIds, Role.INBOX, false));
                            database.overwriteDao()
                                    .insertMailboxOverwrites(
                                            MailboxOverwriteEntity.of(
                                                    threadIds, Role.ARCHIVE, true));
                            deleteQueryItemOverwrite(threadIds, Role.ARCHIVE);
                        }
                    }
                    final WorkManager workManager = WorkManager.getInstance(application);
                    for (final OneTimeWorkRequest workRequest : workRequests) {
                        workManager.enqueueUniqueWork(
                                ModifyLabelsWorker.uniqueName(accountId),
                                ExistingWorkPolicy.APPEND_OR_REPLACE,
                                workRequest);
                    }
                });
        return workRequests.stream().map(WorkRequest::getId).collect(Collectors.toList());
    }
}
