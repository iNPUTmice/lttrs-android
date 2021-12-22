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
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.entity.EntityStateEntity;
import rs.ltt.android.entity.MailboxEntity;
import rs.ltt.android.entity.MailboxOverviewItem;
import rs.ltt.android.entity.MailboxWithRoleAndName;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.mua.cache.Update;

@Dao
public abstract class MailboxDao extends AbstractEntityDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailboxDao.class);

    @Insert
    protected abstract void insert(MailboxEntity mailboxEntity);

    @Insert
    protected abstract void insert(List<MailboxEntity> mailboxEntities);

    @androidx.room.Update
    protected abstract void update(List<MailboxEntity> mailboxEntities);

    @Query("select id from mailbox where role in (:roles) order by role")
    public abstract LiveData<String[]> getMailboxesLiveData(Role... roles);

    @Query("select id from mailbox where role in (:roles) order by role")
    public abstract String[] getMailboxes(Role... roles);

    @Query("select * from mailbox where role is not null")
    public abstract List<MailboxEntity> getSpecialMailboxes();

    @Query("select * from mailbox where name =:name and parentId is null")
    public abstract MailboxEntity getMailboxByNameWhereParentIdIsNull(String name);

    @Query("select * from mailbox where name =:name and parentId =:parentId")
    public abstract MailboxEntity getMailboxByNameAndParent(String name, String parentId);

    @Query("select id,role,name from mailbox where name in(:names)")
    public abstract List<MailboxWithRoleAndName> getMailboxesByNames(final String[] names);

    @Query("select id,parentId,name,sortOrder,unreadThreads,totalEmails,role from mailbox")
    public abstract LiveData<List<MailboxOverviewItem>> getMailboxes();

    @Query(
            "select id,parentId,name,sortOrder,unreadThreads,totalEmails,role from mailbox where"
                    + " role=:role limit 1")
    public abstract LiveData<MailboxOverviewItem> getMailboxOverviewItemLiveData(Role role);

    @Query(
            "select id,parentid,name,sortOrder,unreadThreads,totalEmails,role from mailbox where"
                    + " id=:id")
    public abstract LiveData<MailboxOverviewItem> getMailboxOverviewItemLiveData(String id);

    @Query("select id,role,name from mailbox where role=:role limit 1")
    public abstract ListenableFuture<MailboxWithRoleAndName> getMailboxFuture(Role role);

    @Query("select id,role,name from mailbox where role=:role limit 1")
    public abstract MailboxWithRoleAndName getMailbox(Role role);

    @Query("select id,role,name from mailbox where id=:id limit 1")
    public abstract LiveData<MailboxWithRoleAndName> getMailboxLiveData(String id);

    @Query("select id,role,name from mailbox where role is null or role in (:roles)")
    protected abstract LiveData<List<MailboxWithRoleAndName>> getMailboxesWithRoleEqualsNullOrIn(
            Role... roles);

    public LiveData<List<MailboxWithRoleAndName>> getLabels() {
        return getMailboxesWithRoleEqualsNullOrIn(Role.INBOX);
    }

    @Query(
            "select id,parentid,name,sortOrder,unreadThreads,totalEmails,role from mailbox where"
                    + " id=:id")
    public abstract MailboxOverviewItem getMailbox(String id);

    @Query(
            "select id,parentId,name,sortOrder,unreadThreads,totalEmails,role from mailbox where"
                    + " role=:role limit 1")
    public abstract MailboxOverviewItem getMailboxOverviewItem(Role role);

    @Query(
            "select distinct mailbox.id,role,name from email join email_mailbox on"
                    + " email_mailbox.emailId=email.id join mailbox on"
                    + " email_mailbox.mailboxId=mailbox.id where threadId=:threadId")
    public abstract LiveData<List<MailboxWithRoleAndName>> getMailboxesForThreadLiveData(
            String threadId);

    @Query(
            "select distinct mailbox.id,role,name from email join email_mailbox on"
                    + " email_mailbox.emailId=email.id join mailbox on"
                    + " email_mailbox.mailboxId=mailbox.id where threadId in (:threadIds)")
    public abstract List<MailboxWithRoleAndName> getMailboxesForThreads(
            Collection<String> threadIds);

    @Query(
            "select distinct mailbox.id from email join email_mailbox on"
                    + " email_mailbox.emailId=email.id join mailbox on"
                    + " email_mailbox.mailboxId=mailbox.id where threadId in (:threadIds)")
    public abstract LiveData<List<String>> getMailboxIdsForThreadsLiveData(String[] threadIds);

    @Query(
            "select count((select 1 where not exists(select * from email_mailbox join mailbox on"
                    + " email_mailbox.mailboxId=mailbox.id where mailbox.role=:role and"
                    + " email_mailbox.emailId=email.id))) > 0 from email where threadId=:threadId")
    public abstract LiveData<Boolean> isAnyNotIn(String threadId, Role role);

    @Query("update mailbox set totalEmails=:value where id=:id")
    public abstract void updateTotalEmails(String id, Long value);

    @Query("update mailbox set unreadEmails=:value where id=:id")
    public abstract void updateUnreadEmails(String id, Long value);

    @Query("update mailbox set totalThreads=:value where id=:id")
    public abstract void updateTotalThreads(String id, Long value);

    @Query("update mailbox set unreadThreads=:value where id=:id")
    public abstract void updateUnreadThreads(String id, Long value);

    @Query("delete from mailbox where id=:id")
    public abstract void delete(String id);

    @Query("delete from mailbox")
    public abstract void deleteAll();

    @Transaction
    public void set(List<MailboxEntity> mailboxEntities, String state) {
        if (state != null && state.equals(getState(Mailbox.class))) {
            LOGGER.debug("nothing to do. mailboxes with this state have already been set");
            return;
        }
        deleteAll();
        if (mailboxEntities.size() > 0) {
            insert(mailboxEntities);
        }
        insert(new EntityStateEntity(Mailbox.class, state));
    }

    @Transaction
    public void update(final Update<Mailbox> update, final String[] updatedProperties) {
        final String newState = update.getNewTypedState().getState();
        if (newState != null && newState.equals(getState(Mailbox.class))) {
            LOGGER.debug("nothing to do. mailboxes already at newest state");
            return;
        }
        for (Mailbox mailbox : update.getCreated()) {
            insert(MailboxEntity.of(mailbox));
        }
        if (updatedProperties == null) {
            List<MailboxEntity> updatedEntities = new ArrayList<>();
            for (Mailbox mailbox : update.getUpdated()) {
                updatedEntities.add(MailboxEntity.of(mailbox));
            }
            update(updatedEntities);
        } else {
            for (Mailbox mailbox : update.getUpdated()) {
                for (String property : updatedProperties) {
                    switch (property) {
                        case "totalEmails":
                            updateTotalEmails(mailbox.getId(), mailbox.getTotalEmails());
                            break;
                        case "unreadEmails":
                            updateUnreadEmails(mailbox.getId(), mailbox.getUnreadEmails());
                            break;
                        case "totalThreads":
                            updateTotalThreads(mailbox.getId(), mailbox.getTotalThreads());
                            break;
                        case "unreadThreads":
                            updateUnreadThreads(mailbox.getId(), mailbox.getUnreadThreads());
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    "Unable to update property '" + property + "'");
                    }
                }
            }
        }
        for (String id : update.getDestroyed()) {
            delete(id);
        }
        throwOnUpdateConflict(Mailbox.class, update.getOldTypedState(), update.getNewTypedState());
    }
}
