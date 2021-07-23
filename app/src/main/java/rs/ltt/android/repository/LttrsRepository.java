package rs.ltt.android.repository;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import rs.ltt.android.database.AppDatabase;
import rs.ltt.android.entity.KeywordOverwriteEntity;
import rs.ltt.android.entity.MailboxOverviewItem;
import rs.ltt.android.entity.MailboxOverwriteEntity;
import rs.ltt.android.entity.MailboxWithRoleAndName;
import rs.ltt.android.ui.EmptyMailboxAction;
import rs.ltt.android.util.Event;
import rs.ltt.android.util.MainThreadExecutor;
import rs.ltt.android.worker.AbstractMuaWorker;
import rs.ltt.android.worker.ArchiveWorker;
import rs.ltt.android.worker.CopyToMailboxWorker;
import rs.ltt.android.worker.EmptyTrashWorker;
import rs.ltt.android.worker.Failure;
import rs.ltt.android.worker.MarkImportantWorker;
import rs.ltt.android.worker.ModifyKeywordWorker;
import rs.ltt.android.worker.MoveToInboxWorker;
import rs.ltt.android.worker.MoveToTrashWorker;
import rs.ltt.android.worker.RemoveFromMailboxWorker;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRole;
import rs.ltt.jmap.common.entity.Keyword;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.mua.util.KeywordUtil;

public class LttrsRepository extends AbstractRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(LttrsRepository.class);

    private final MediatorLiveData<Event<Failure>> failureEventMediator = new MediatorLiveData<>();

    public LttrsRepository(Application application, long accountId) {
        super(application, accountId);
    }

    public LiveData<List<MailboxOverviewItem>> getMailboxes() {
        return database.mailboxDao().getMailboxes();
    }

    public LiveData<Event<Failure>> getFailureEvent() {
        return this.failureEventMediator;
    }

    public void removeFromMailbox(final Collection<String> threadIds, final IdentifiableMailboxWithRole mailbox) {
        IO_EXECUTOR.execute(() -> {
            if (mailbox.getRole() == Role.IMPORTANT) {
                markNotImportant(threadIds, mailbox);
                return;
            }
            insertQueryItemOverwrite(threadIds, mailbox);
            for (final String threadId : threadIds) {
                final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(RemoveFromMailboxWorker.class)
                        .setConstraints(CONNECTED_CONSTRAINT)
                        .setInputData(RemoveFromMailboxWorker.data(accountId, threadId, mailbox))
                        .addTag(AbstractMuaWorker.TAG_EMAIL_MODIFICATION)
                        .build();
                dispatchWorkRequest(workRequest);
            }
        });
    }

    public void copyToMailbox(@NonNull final Collection<String> threadIds, @NonNull final IdentifiableMailboxWithRole mailbox) {
        if (mailbox.getRole() == Role.IMPORTANT) {
            markImportant(threadIds);
            return;
        }
        IO_EXECUTOR.execute(() -> {
            deleteQueryItemOverwrite(threadIds, mailbox);
            for (final String threadId : threadIds) {
                final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(CopyToMailboxWorker.class)
                        .setConstraints(CONNECTED_CONSTRAINT)
                        .setInputData(CopyToMailboxWorker.data(accountId, threadId, mailbox))
                        .addTag(AbstractMuaWorker.TAG_EMAIL_MODIFICATION)
                        .build();
                dispatchWorkRequest(workRequest);
            }
        });
    }

    public void archive(final Collection<String> threadIds) {
        IO_EXECUTOR.execute(() -> {
            insertQueryItemOverwrite(threadIds, Role.INBOX);
            deleteQueryItemOverwrite(threadIds, Role.ARCHIVE);
            database.overwriteDao().insertMailboxOverwrites(MailboxOverwriteEntity.of(threadIds, Role.INBOX, false));
            database.overwriteDao().insertMailboxOverwrites(MailboxOverwriteEntity.of(threadIds, Role.ARCHIVE, true));
            for (final String threadId : threadIds) {
                final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ArchiveWorker.class)
                        .setConstraints(CONNECTED_CONSTRAINT)
                        .setInputData(ArchiveWorker.data(accountId, threadId))
                        .addTag(AbstractMuaWorker.TAG_EMAIL_MODIFICATION)
                        .build();
                dispatchWorkRequest(workRequest);
            }
        });
    }

    public void moveToInbox(final Collection<String> threadIds) {
        IO_EXECUTOR.execute(() -> {
            insertQueryItemOverwrite(threadIds, Role.ARCHIVE);
            insertQueryItemOverwrite(threadIds, Role.TRASH);
            deleteQueryItemOverwrite(threadIds, Role.INBOX);

            database.overwriteDao().insertMailboxOverwrites(
                    MailboxOverwriteEntity.of(threadIds, Role.INBOX, true)
            );
            database.overwriteDao().insertMailboxOverwrites(
                    MailboxOverwriteEntity.of(threadIds, Role.ARCHIVE, false)
            );
            database.overwriteDao().insertMailboxOverwrites(
                    MailboxOverwriteEntity.of(threadIds, Role.TRASH, false)
            );
            for (final String threadId : threadIds) {
                final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MoveToInboxWorker.class)
                        .setConstraints(CONNECTED_CONSTRAINT)
                        .setInputData(MoveToInboxWorker.data(accountId, threadId))
                        .addTag(AbstractMuaWorker.TAG_EMAIL_MODIFICATION)
                        .build();
                dispatchWorkRequest(workRequest);
            }
        });
    }

    //TODO check if we can return LiveData<WorkInfo> directly by constructing the workRequest before the executor
    public ListenableFuture<LiveData<WorkInfo>> moveToTrash(final Collection<String> threadIds) {
        final SettableFuture<LiveData<WorkInfo>> future = SettableFuture.create();
        IO_EXECUTOR.execute(() -> {
            for (MailboxWithRoleAndName mailbox : database.mailboxDao().getMailboxesForThreads(threadIds)) {
                if (mailbox.role != Role.TRASH) {
                    insertQueryItemOverwrite(threadIds, mailbox);
                }
            }
            for (String keyword : KeywordUtil.KEYWORD_ROLE.keySet()) {
                insertQueryItemOverwrite(threadIds, keyword);
            }
            final AppDatabase appDatabase = AppDatabase.getInstance(application);
            for (final String searchQuery : appDatabase.searchSuggestionDao().getSearchQueries()) {
                insertSearchQueryItemOverwrite(threadIds, searchQuery);
            }
            database.overwriteDao().insertMailboxOverwrites(
                    MailboxOverwriteEntity.of(threadIds, Role.INBOX, false)
            );
            database.overwriteDao().insertMailboxOverwrites(
                    MailboxOverwriteEntity.of(threadIds, Role.TRASH, true)
            );
            final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MoveToTrashWorker.class)
                    .setConstraints(CONNECTED_CONSTRAINT)
                    .setInputData(MoveToTrashWorker.data(accountId, threadIds))
                    .setInitialDelay(5, TimeUnit.SECONDS)
                    .addTag(AbstractMuaWorker.TAG_EMAIL_MODIFICATION)
                    .build();
            future.set(dispatchWorkRequest(workRequest));
        });
        return future;
    }

    public void cancelMoveToTrash(final WorkInfo workInfo, final Collection<String> threadIds) {
        Preconditions.checkNotNull(workInfo, "Unable to cancel moveToTrash operation.");
        WorkManager.getInstance(application).cancelWorkById(workInfo.getId());
        IO_EXECUTOR.execute(() -> {
            database.overwriteDao().revertMoveToTrashOverwrites(threadIds);
        });
    }

    public void markImportant(final Collection<String> threadIds) {
        IO_EXECUTOR.execute(() -> {
            database.overwriteDao().insertMailboxOverwrites(
                    MailboxOverwriteEntity.of(threadIds, Role.IMPORTANT, true)
            );
            deleteQueryItemOverwrite(threadIds, Role.IMPORTANT);
            for (final String threadId : threadIds) {
                final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MarkImportantWorker.class)
                        .setConstraints(CONNECTED_CONSTRAINT)
                        .setInputData(MarkImportantWorker.data(accountId, threadId))
                        .addTag(AbstractMuaWorker.TAG_EMAIL_MODIFICATION)
                        .build();
                dispatchWorkRequest(workRequest);
            }
        });
    }

    public void markNotImportant(final Collection<String> threadIds) {
        IO_EXECUTOR.execute(() -> {
            final MailboxWithRoleAndName mailbox = Preconditions.checkNotNull(
                    database.mailboxDao().getMailbox(Role.IMPORTANT),
                    "No mailbox with role=IMPORTANT found in cache"
            );
            markNotImportant(threadIds, mailbox);
        });
    }

    private void markNotImportant(final Collection<String> threadIds, final IdentifiableMailboxWithRole mailbox) {
        Preconditions.checkArgument(mailbox.getRole() == Role.IMPORTANT);
        insertQueryItemOverwrite(threadIds, mailbox);
        database.overwriteDao().insertMailboxOverwrites(
                MailboxOverwriteEntity.of(threadIds, Role.IMPORTANT, false)
        );
        for (final String threadId : threadIds) {
            final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(RemoveFromMailboxWorker.class)
                    .setConstraints(CONNECTED_CONSTRAINT)
                    .setInputData(RemoveFromMailboxWorker.data(accountId, threadId, mailbox))
                    .addTag(AbstractMuaWorker.TAG_EMAIL_MODIFICATION)
                    .build();
            dispatchWorkRequest(workRequest);
        }
    }

    public void toggleFlagged(final Collection<String> threadIds, final boolean targetState) {
        toggleKeyword(threadIds, Keyword.FLAGGED, targetState);
    }

    public void removeKeyword(final Collection<String> threadIds, final String keyword) {
        toggleKeyword(threadIds, keyword, false);
    }

    public void addKeyword(final Collection<String> threadIds, final String keyword) {
        toggleKeyword(threadIds, keyword, true);
    }

    private void toggleKeyword(final Collection<String> threadIds, final String keyword, final boolean targetState) {
        Preconditions.checkNotNull(threadIds);
        Preconditions.checkNotNull(keyword);
        LOGGER.info("toggle keyword {} for threads {}", keyword, threadIds);
        IO_EXECUTOR.execute(() -> {
            final Collection<KeywordOverwriteEntity> entities = Collections2.transform(
                    threadIds,
                    threadId -> new KeywordOverwriteEntity(threadId, keyword, targetState)
            );
            insert(entities);
            if (targetState) {
                deleteQueryItemOverwrite(threadIds, keyword);
            } else {
                insertQueryItemOverwrite(threadIds, keyword);
            }
            for (final String threadId : threadIds) {
                final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ModifyKeywordWorker.class)
                        .setConstraints(CONNECTED_CONSTRAINT)
                        .setInputData(ModifyKeywordWorker.data(accountId, threadId, keyword, targetState))
                        .addTag(AbstractMuaWorker.TAG_EMAIL_MODIFICATION)
                        .build();
                dispatchWorkRequest(workRequest);
            }
        });
    }

    public void markRead(final Collection<String> threadIds) {
        toggleKeyword(threadIds, Keyword.SEEN, true);
    }

    public void markUnRead(final Collection<String> threadIds) {
        toggleKeyword(threadIds, Keyword.SEEN, false);
    }

    protected LiveData<WorkInfo> dispatchWorkRequest(final OneTimeWorkRequest workRequest) {
        final WorkManager workManager = WorkManager.getInstance(application);
        workManager.enqueueUniqueWork(
                AbstractMuaWorker.uniqueName(accountId),
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                workRequest
        );
        return observeForFailure(workRequest);
    }

    public void executeEmptyMailboxAction(final EmptyMailboxAction action) {
        final Role role = action.getRole();
        if (role == Role.TRASH) {
            emptyTrash();
        } else {
            throw new IllegalStateException(String.format("Emptying %s has not been implemented", role));
        }
    }

    private void emptyTrash() {
        final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(EmptyTrashWorker.class)
                .setConstraints(CONNECTED_CONSTRAINT)
                .setInputData(EmptyTrashWorker.data(accountId))
                .build();
        dispatchWorkRequest(workRequest);
    }

    protected LiveData<WorkInfo> observeForFailure(final OneTimeWorkRequest workRequest) {
        return observeForFailure(workRequest.getId());
    }

    public void observeForFailure(List<UUID> ids) {
        for (final UUID id : ids) {
            observeForFailure(id);
        }
    }

    public LiveData<WorkInfo> observeForFailure(final UUID id) {
        final WorkManager workManager = WorkManager.getInstance(application);
        final LiveData<WorkInfo> workInfoLiveData = workManager.getWorkInfoByIdLiveData(id);
        MainThreadExecutor.getInstance().execute(() -> failureEventMediator.addSource(workInfoLiveData, workInfo -> {
            if (workInfo.getState().isFinished()) {
                failureEventMediator.removeSource(workInfoLiveData);
            }
            if (workInfo.getState() == WorkInfo.State.FAILED) {
                final Data data = workInfo.getOutputData();
                try {
                    failureEventMediator.postValue(new Event<>(Failure.of(data)));
                } catch (final IllegalArgumentException e) {
                    LOGGER.warn("Work info failure not recognized {}", data);
                }
            }
        }));
        return workInfoLiveData;
    }
}
