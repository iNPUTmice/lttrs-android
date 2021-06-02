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
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedList;
import androidx.work.Data;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import rs.ltt.android.entity.EmailComplete;
import rs.ltt.android.entity.ExpandedPosition;
import rs.ltt.android.entity.MailboxOverwriteEntity;
import rs.ltt.android.entity.MailboxWithRoleAndName;
import rs.ltt.android.entity.Seen;
import rs.ltt.android.entity.SubjectWithImportance;
import rs.ltt.android.entity.ThreadHeader;
import rs.ltt.android.repository.ThreadViewRepository;
import rs.ltt.android.util.CombinedListsLiveData;
import rs.ltt.android.util.Event;
import rs.ltt.jmap.common.entity.Role;

public class ThreadViewModel extends AndroidViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadViewModel.class);

    public final AtomicBoolean jumpedToFirstUnread = new AtomicBoolean(false);
    public final ListenableFuture<List<ExpandedPosition>> expandedPositions;
    public final MutableLiveData<Event<Seen>> seenEvent = new MutableLiveData<>();
    public final HashSet<String> expandedItems = new HashSet<>();
    private final String threadId;
    private final String label;
    private final ThreadViewRepository threadViewRepository;
    private final MediatorLiveData<Event<String>> threadViewRedirect = new MediatorLiveData<>();
    private LiveData<PagedList<EmailComplete>> emails;
    private final MediatorLiveData<SubjectWithImportance> subjectWithImportance = new MediatorLiveData<>();
    private LiveData<Boolean> flagged;
    private LiveData<List<MailboxWithRoleAndName>> mailboxes;
    private LiveData<MenuConfiguration> menuConfiguration;


    ThreadViewModel(@NonNull final Application application,
                    final long accountId,
                    final String threadId,
                    final String label) {
        super(application);
        this.threadId = threadId;
        this.label = label;
        this.threadViewRepository = new ThreadViewRepository(application, accountId);
        final LiveData<ThreadHeader> header = this.threadViewRepository.getThreadHeader(threadId);
        this.emails = this.threadViewRepository.getEmails(threadId);
        this.mailboxes = this.threadViewRepository.getMailboxes(threadId);
        final ListenableFuture<Seen> seen = this.threadViewRepository.getSeen(threadId);
        this.expandedPositions = Futures.transform(seen, Seen::getExpandedPositions, MoreExecutors.directExecutor());
        Futures.addCallback(seen, new FutureCallback<Seen>() {
            @Override
            public void onSuccess(@Nullable Seen seen) {
                if (seen != null) {
                    seenEvent.postValue(new Event<>(seen));
                }
            }

            @Override
            public void onFailure(@NotNull Throwable t) {

            }
        }, MoreExecutors.directExecutor());

        LiveData<List<MailboxOverwriteEntity>> overwriteEntityLiveData = this.threadViewRepository.getMailboxOverwrites(threadId);

        CombinedListsLiveData<MailboxOverwriteEntity, MailboxWithRoleAndName> combined = new CombinedListsLiveData<>(overwriteEntityLiveData, mailboxes);

        this.menuConfiguration = Transformations.map(combined, pair -> {
            List<MailboxOverwriteEntity> overwrites = pair.first;
            List<MailboxWithRoleAndName> list = pair.second;
            boolean wasPutInArchiveOverwrite = MailboxOverwriteEntity.hasOverwrite(overwrites, Role.ARCHIVE);
            boolean wasPutInTrashOverwrite = MailboxOverwriteEntity.hasOverwrite(overwrites, Role.TRASH);
            boolean wasPutInInboxOverwrite = MailboxOverwriteEntity.hasOverwrite(overwrites, Role.INBOX);
            final MailboxOverwriteEntity importantOverwrite = MailboxOverwriteEntity.find(overwrites, Role.IMPORTANT);

            final boolean removeLabel = MailboxWithRoleAndName.isAnyOfLabel(list, this.label);
            final boolean archive = !removeLabel && (MailboxWithRoleAndName.isAnyOfRole(list, Role.INBOX) || wasPutInInboxOverwrite) && !wasPutInArchiveOverwrite && !wasPutInTrashOverwrite;
            final boolean moveToInbox = (MailboxWithRoleAndName.isAnyOfRole(list, Role.ARCHIVE) || MailboxWithRoleAndName.isAnyOfRole(list, Role.TRASH) || wasPutInArchiveOverwrite || wasPutInTrashOverwrite) && !wasPutInInboxOverwrite;
            final boolean moveToTrash = (MailboxWithRoleAndName.isAnyNotOfRole(list, Role.TRASH) || wasPutInInboxOverwrite) && !wasPutInTrashOverwrite;

            final boolean markedAsImportant = importantOverwrite != null ? importantOverwrite.value : MailboxWithRoleAndName.isAnyOfRole(list, Role.IMPORTANT);

            return new MenuConfiguration(archive,
                    removeLabel,
                    moveToInbox,
                    moveToTrash,
                    !markedAsImportant,
                    markedAsImportant
            );
        });

        final LiveData<Boolean> importance = Transformations.map(combined, pair -> {
            final List<MailboxOverwriteEntity> overwrites = pair.first;
            final List<MailboxWithRoleAndName> list = pair.second;
            final MailboxOverwriteEntity importantOverwrite = MailboxOverwriteEntity.find(overwrites, Role.IMPORTANT);
            return importantOverwrite != null ? importantOverwrite.value : MailboxWithRoleAndName.isAnyOfRole(list, Role.IMPORTANT);
        });

        this.subjectWithImportance.addSource(importance, important -> setSubjectWithImportance(header.getValue(), important));
        this.subjectWithImportance.addSource(header, threadHeader -> setSubjectWithImportance(threadHeader, importance.getValue()));

        this.flagged = Transformations.map(header, h -> h != null && h.showAsFlagged());

        //TODO add LiveData that is true when header != null and display 'Thread not found' or something in UI
    }

    private void setSubjectWithImportance(ThreadHeader header, Boolean important) {
        if (header == null || important == null) {
            return;
        }
        this.subjectWithImportance.postValue(SubjectWithImportance.of(header, important));
    }

    public LiveData<Event<String>> getThreadViewRedirect() {
        return this.threadViewRedirect;
    }

    public LiveData<PagedList<EmailComplete>> getEmails() {
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

    public String getThreadId() {
        return this.threadId;
    }

    public MailboxWithRoleAndName getMailbox() {
        final List<MailboxWithRoleAndName> mailboxes = this.mailboxes.getValue();
        final MailboxWithRoleAndName mailbox = mailboxes == null ? null : MailboxWithRoleAndName.findByLabel(mailboxes, this.label);
        if (mailbox == null) {
            throw new IllegalStateException("No mailbox found with the label " + this.label);
        }
        return mailbox;
    }

    public void waitForEdit(UUID uuid) {
        final WorkManager workManager = WorkManager.getInstance(getApplication());
        final LiveData<WorkInfo> liveData = workManager.getWorkInfoByIdLiveData(uuid);
        threadViewRedirect.addSource(liveData, workInfo -> {
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

    public static class MenuConfiguration {
        public final boolean archive;
        public final boolean removeLabel;
        public final boolean moveToInbox;
        public final boolean moveToTrash;
        public final boolean markImportant;
        public final boolean markNotImportant;

        MenuConfiguration(boolean archive, boolean removeLabel, boolean moveToInbox, boolean moveToTrash, boolean markImportant, boolean markNotImportant) {
            this.archive = archive;
            this.removeLabel = removeLabel;
            this.moveToInbox = moveToInbox;
            this.moveToTrash = moveToTrash;
            this.markImportant = markImportant;
            this.markNotImportant = markNotImportant;
        }

        public static MenuConfiguration none() {
            return new MenuConfiguration(false,
                    false,
                    false,
                    false,
                    false,
                    false
            );
        }
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final Application application;
        private final long accountId;
        private final String threadId;
        private final String label;

        public Factory(final Application application,
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
            return Objects.requireNonNull(modelClass.cast(new ThreadViewModel(application, accountId, threadId, label)));
        }
    }

}
