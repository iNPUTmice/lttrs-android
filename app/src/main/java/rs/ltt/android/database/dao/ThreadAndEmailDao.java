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

package rs.ltt.android.database.dao;

import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.entity.DownloadableBlob;
import rs.ltt.android.entity.EmailBodyPartEntity;
import rs.ltt.android.entity.EmailBodyValueEntity;
import rs.ltt.android.entity.EmailEmailAddressEntity;
import rs.ltt.android.entity.EmailEntity;
import rs.ltt.android.entity.EmailInReplyToEntity;
import rs.ltt.android.entity.EmailKeywordEntity;
import rs.ltt.android.entity.EmailMailboxEntity;
import rs.ltt.android.entity.EmailMessageIdEntity;
import rs.ltt.android.entity.EmailWithBodies;
import rs.ltt.android.entity.EmailWithBodiesAndSubject;
import rs.ltt.android.entity.EmailWithEncryptionStatus;
import rs.ltt.android.entity.EmailWithKeywords;
import rs.ltt.android.entity.EmailWithMailboxes;
import rs.ltt.android.entity.EmailWithReferences;
import rs.ltt.android.entity.EncryptedEmail;
import rs.ltt.android.entity.EncryptionStatus;
import rs.ltt.android.entity.EntityStateEntity;
import rs.ltt.android.entity.ExpandedPosition;
import rs.ltt.android.entity.ThreadEntity;
import rs.ltt.android.entity.ThreadHeader;
import rs.ltt.android.entity.ThreadItemEntity;
import rs.ltt.android.util.TextBodies;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.Thread;
import rs.ltt.jmap.common.entity.TypedState;
import rs.ltt.jmap.mua.cache.Missing;
import rs.ltt.jmap.mua.cache.Update;

@Dao
public abstract class ThreadAndEmailDao extends AbstractEntityDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadAndEmailDao.class);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void insert(ThreadEntity entity);

    @Insert
    abstract void insert(List<ThreadItemEntity> entities);

    @Query("delete from thread_item where threadId=:threadId")
    abstract void deleteAllThreadItem(String threadId);

    @Delete
    abstract void delete(ThreadEntity thread);

    @Query("delete from thread")
    abstract void deleteAllThread();

    private void set(Thread[] threads, String state) {
        deleteAllThread();
        if (threads.length > 0) {
            insertThreads(threads);
        }
        insert(new EntityStateEntity(Thread.class, state));
    }

    private void add(final TypedState<Thread> expectedState, Thread[] threads) {
        if (threads.length > 0) {
            insertThreads(threads);
        }
        throwOnCacheConflict(Thread.class, expectedState);
    }

    private void insertThreads(Thread[] threads) {
        for (Thread thread : threads) {
            insert(ThreadEntity.of(thread));
            insert(ThreadItemEntity.of(thread));
        }
    }

    @Query("SELECT EXISTS(SELECT 1 FROM thread WHERE threadId=:threadId)")
    protected abstract boolean threadExists(String threadId);

    @Transaction
    public void update(Update<Thread> update) {
        final String newState = update.getNewTypedState().getState();
        if (newState != null && newState.equals(getState(Thread.class))) {
            LOGGER.debug("nothing to do. threads already at newest state");
            return;
        }
        final Thread[] created = update.getCreated();
        if (created.length > 0) {
            insertThreads(created);
        }
        for (final Thread thread : update.getUpdated()) {
            if (threadExists(thread.getId())) {
                deleteAllThreadItem(thread.getId());
                insert(ThreadItemEntity.of(thread));
            } else {
                LOGGER.debug("skipping update to thread " + thread.getId());
            }
        }
        for (final String id : update.getDestroyed()) {
            delete(ThreadEntity.of(id));
        }
        throwOnUpdateConflict(Thread.class, update.getOldTypedState(), update.getNewTypedState());
    }

    @Query(
            " select threadId from `query` join query_item on `query`.id = queryId where threadId"
                    + " not in(select thread.threadId from thread) and queryString=:queryString")
    public abstract List<String> getMissingThreadIds(String queryString);

    @Transaction
    public Missing getMissing(String queryString) {
        final List<String> ids = getMissingThreadIds(queryString);
        final String threadState = getState(Thread.class);
        final String emailState = getState(Email.class);
        return new Missing(threadState, emailState, ids);
    }

    @Query("delete from email where id=:id")
    abstract void deleteEmail(String id);

    @Query("delete from email_keyword where emailId=:emailId")
    abstract void deleteKeywords(String emailId);

    @Query("delete from email_mailbox where emailId=:emailId")
    abstract void deleteMailboxes(String emailId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void insert(EmailEntity entity);

    @Query("update email set encryptionStatus=:status where id=:emailId")
    public abstract void setEncryptionStatus(final String emailId, final EncryptionStatus status);

    @Query("update email set preview=:preview where id=:emailId")
    abstract void setPreview(final String emailId, final String preview);

    @Insert
    abstract void insertEmailAddresses(List<EmailEmailAddressEntity> entities);

    @Insert
    abstract void insertInReplyTo(List<EmailInReplyToEntity> entities);

    @Insert
    abstract void insertMessageId(List<EmailMessageIdEntity> entities);

    @Insert
    abstract void insertMailboxes(List<EmailMailboxEntity> entities);

    @Insert
    abstract void insertKeywords(List<EmailKeywordEntity> entities);

    @Insert
    abstract void insertEmailBodyValues(List<EmailBodyValueEntity> entities);

    @Query("delete from email_body_value where emailId=:emailId")
    abstract void deleteEmailBodyValues(final String emailId);

    @Insert
    abstract void insertEmailBodyParts(List<EmailBodyPartEntity> entities);

    @Query(
            "update email_body_part set downloadCount = downloadCount + 1 where emailId=:emailId"
                    + " and blobId=:blobId")
    public abstract void incrementEmailBodyPartDownloadCount(
            final String emailId, final String blobId);

    @Query("delete from email_body_part where emailId=:emailId")
    abstract void deleteEmailBodyParts(final String emailId);

    @Query("select threadId from email where id=:emailId")
    public abstract String getThreadId(String emailId);

    @Query("select threadId from email where id=:emailId")
    public abstract LiveData<String> getThreadIdLiveData(String emailId);

    @Transaction
    @Query("select id from email where threadId=:threadId")
    public abstract List<EmailWithKeywords> getEmailsWithKeywords(String threadId);

    @Transaction
    @Query("select id from email where threadId=:threadId")
    public abstract List<EmailWithMailboxes> getEmailsWithMailboxes(String threadId);

    @Transaction
    @Query("select id from email where threadId in (:threadIds)")
    public abstract List<EmailWithMailboxes> getEmailsWithMailboxes(Collection<String> threadIds);

    @Transaction
    @Query("select id from email where id=:id")
    public abstract EmailWithKeywords getEmailWithKeyword(final String id);

    @Query("select id,encryptionStatus,encryptedBlobId from email where id=:id")
    public abstract EmailWithEncryptionStatus getEmailWithEncryptionStatus(final String id);

    @Transaction
    @Query(
            "select id,threadId,receivedAt,sentAt,encryptionStatus,encryptedBlobId from email"
                    + " where id=:id")
    public abstract EncryptedEmail getEncryptedEmail(final String id);

    @Query(
            "select id,encryptionStatus,encryptedBlobId from email where threadId=:threadId and"
                    + " encryptionStatus=:encryptionStatus")
    public abstract LiveData<List<EmailWithEncryptionStatus>> getEmailsWithEncryptionStatus(
            final String threadId, final EncryptionStatus encryptionStatus);

    @Query(
            "select blobId,type,name,size from email_body_part where emailId=:emailId and"
                    + " blobId=:blobId")
    public abstract ListenableFuture<DownloadableBlob> getDownloadable(
            String emailId, String blobId);

    @Transaction
    @Query(
            "select id,receivedAt,sentAt,email.threadId,encryptionStatus from thread_item"
                    + " join email on thread_item.emailId=email.id where"
                    + " thread_item.threadId=:threadId order by position")
    public abstract DataSource.Factory<Integer, EmailWithBodies> getEmails(String threadId);

    @Query("select emailId from thread_item where threadId in (:threadIds)")
    public abstract ListenableFuture<List<String>> getEmailIds(final Collection<String> threadIds);

    @Transaction
    @Query(
            "select id,receivedAt,sentAt,threadId,subject,encryptionStatus from email where id in"
                    + " (:emailIds)")
    public abstract List<EmailWithBodiesAndSubject> getEmails(Collection<String> emailIds);

    // TODO remove 'preview'. 'receivedAt' is strictly speaking not necessary currently but might be
    // needed in the future for quoting the original email
    @Transaction
    @Query(
            "select :accountId as accountId,id,threadId,subject,receivedAt,sentAt,encryptionStatus"
                    + " from email where id=:id")
    public abstract ListenableFuture<EmailWithReferences> getEmailWithReferences(
            Long accountId, String id);

    @Transaction
    @Query(
            "select subject,email.threadId from thread_item join email on"
                    + " thread_item.emailId=email.id where thread_item.threadId=:threadId order by"
                    + " position limit 1")
    public abstract LiveData<ThreadHeader> getThreadHeader(String threadId);

    @Query(
            "select position,emailId from thread_item where threadId=:threadId and"
                + " thread_item.emailId not in (select thread_item.emailId from thread_item join"
                + " email_keyword on thread_item.emailId=email_keyword.emailId where"
                + " threadId=:threadId and email_keyword.keyword='$seen') order by position")
    public abstract ListenableFuture<List<ExpandedPosition>> getUnseenPositions(String threadId);

    @Query("select position,emailId from thread_item where threadId=:threadId order by position")
    public abstract ListenableFuture<List<ExpandedPosition>> getAllPositions(String threadId);

    @Query(
            "select position,emailId from thread_item where threadId=:threadId order by position"
                    + " desc limit 1")
    public abstract ListenableFuture<List<ExpandedPosition>> getMaxPosition(String threadId);

    @Query("delete from email")
    abstract void deleteAllEmail();

    private void set(final Email[] emails, final String state) {
        deleteAllEmail();
        if (emails.length > 0) {
            insertEmails(emails);
        }
        insert(new EntityStateEntity(Email.class, state));
    }

    @Query(
            "delete from keyword_overwrite where threadId=(select threadId from email where"
                    + " id=:emailId)")
    protected abstract void deleteKeywordToggle(String emailId);

    @Query(
            "delete from mailbox_overwrite where threadId=(select threadId from email where"
                    + " id=:emailId)")
    protected abstract void deleteMailboxOverwrite(String emailId);

    @Query(
            "update query_item_overwrite set executed=1 where executed=0 and threadId IN(select"
                    + " email.threadid from email where email.id=:emailId)")
    protected abstract int markAsExecuted(String emailId);

    @Transaction
    public void add(
            final TypedState<Thread> expectedThreadState,
            Thread[] threads,
            final TypedState<Email> expectedEmailState,
            final Email[] emails) {
        add(expectedThreadState, threads);
        add(expectedEmailState, emails);
    }

    @Transaction
    public void set(
            final TypedState<Thread> threadState,
            Thread[] threads,
            final TypedState<Email> emailState,
            final Email[] emails) {
        set(threads, threadState.getState());
        set(emails, emailState.getState());
    }

    private void add(final TypedState<Email> expectedState, Email[] email) {
        if (email.length > 0) {
            insertEmails(email);
        }
        throwOnCacheConflict(Email.class, expectedState);
    }

    @Query("SELECT EXISTS(SELECT 1 FROM email WHERE id=:emailId)")
    protected abstract boolean emailExists(String emailId);

    private void insertEmails(final Email[] emails) {
        for (final Email email : emails) {
            final List<EmailBodyPartEntity> bodyPartEntities = EmailBodyPartEntity.of(email);
            final List<EmailBodyValueEntity> bodyValueEntities = EmailBodyValueEntity.of(email);
            final Email emailWithPreview =
                    email.toBuilder()
                            .preview(TextBodies.getPreview(bodyPartEntities, bodyValueEntities))
                            .build();
            insert(EmailEntity.of(emailWithPreview));
            insertInReplyTo(EmailInReplyToEntity.of(email));
            insertMessageId(EmailMessageIdEntity.of(email));
            insertEmailAddresses(EmailEmailAddressEntity.of(email));
            insertMailboxes(EmailMailboxEntity.of(email));
            insertKeywords(EmailKeywordEntity.of(email));
            insertEmailBodyParts(bodyPartEntities);
            insertEmailBodyValues(bodyValueEntities);
        }
    }

    @Transaction
    public void updateEmails(final Update<Email> update, final String[] updatedProperties) {
        final String newState = update.getNewTypedState().getState();
        if (newState != null && newState.equals(getState(Email.class))) {
            LOGGER.debug("nothing to do. emails already at newest state");
            return;
        }
        final Email[] created = update.getCreated();
        if (created.length > 0) {
            insertEmails(created);
        }
        if (updatedProperties != null) {
            for (final Email email : update.getUpdated()) {
                if (!emailExists(email.getId())) {
                    LOGGER.warn(
                            "skipping updates to email {} because we don’t have that",
                            email.getId());
                    continue;
                }
                for (final String property : updatedProperties) {
                    switch (property) {
                        case "keywords":
                            deleteKeywords(email.getId());
                            insertKeywords(EmailKeywordEntity.of(email));
                            break;
                        case "mailboxIds":
                            deleteMailboxes(email.getId());
                            insertMailboxes(EmailMailboxEntity.of(email));
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    "Unable to update property '" + property + "'");
                    }
                }
                deleteOverwrites(email.getId());
            }
        }
        for (final String id : update.getDestroyed()) {
            deleteEmail(id);
        }
        throwOnUpdateConflict(Email.class, update.getOldTypedState(), update.getNewTypedState());
    }

    private void deleteOverwrites(final String emailId) {
        deleteKeywordToggle(emailId);
        deleteMailboxOverwrite(emailId);
        final int executed = markAsExecuted(emailId);
        if (executed > 0) {
            LOGGER.info("Marked {} query item overwrites as executed", executed);
        }
    }

    @Transaction
    public void setPlaintextBodyParts(final Email email) {
        Preconditions.checkNotNull(email.getId(), "Email must contain an ID");
        deleteEmailBodyParts(email.getId());
        final List<EmailBodyPartEntity> bodyPartEntities = EmailBodyPartEntity.of(email);
        insertEmailBodyParts(bodyPartEntities);
        deleteEmailBodyValues(email.getId());
        final List<EmailBodyValueEntity> bodyValueEntities = EmailBodyValueEntity.of(email);
        insertEmailBodyValues(bodyValueEntities);
        final String preview = TextBodies.getPreview(bodyPartEntities, bodyValueEntities);
        setEncryptionStatus(email.getId(), EncryptionStatus.PLAINTEXT);
        setPreview(email.getId(), preview);
    }
}
