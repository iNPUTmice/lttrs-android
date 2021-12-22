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

package rs.ltt.android.cache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.database.LttrsDatabase;
import rs.ltt.android.entity.MailboxEntity;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRole;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRoleAndName;
import rs.ltt.jmap.common.entity.Identity;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.Thread;
import rs.ltt.jmap.common.entity.TypedState;
import rs.ltt.jmap.mua.cache.Cache;
import rs.ltt.jmap.mua.cache.Missing;
import rs.ltt.jmap.mua.cache.ObjectsState;
import rs.ltt.jmap.mua.cache.QueryStateWrapper;
import rs.ltt.jmap.mua.cache.QueryUpdate;
import rs.ltt.jmap.mua.cache.Update;
import rs.ltt.jmap.mua.cache.exception.CacheConflictException;
import rs.ltt.jmap.mua.cache.exception.CacheReadException;
import rs.ltt.jmap.mua.cache.exception.CacheWriteException;
import rs.ltt.jmap.mua.cache.exception.NotSynchronizedException;
import rs.ltt.jmap.mua.util.QueryResult;
import rs.ltt.jmap.mua.util.QueryResultItem;

public class DatabaseCache implements Cache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCache.class);

    private final LttrsDatabase database;

    public DatabaseCache(LttrsDatabase database) {
        this.database = database;
    }

    @Override
    public String getIdentityState() {
        return database.identityDao().getState(Identity.class);
    }

    @Override
    public String getMailboxState() {
        return database.mailboxDao().getState(Identity.class);
    }

    @NonNull
    @Override
    public QueryStateWrapper getQueryState(@Nullable String query) {
        return database.stateDao().getQueryStateWrapper(query);
    }

    @NonNull
    @Override
    public ObjectsState getObjectsState() {
        return database.stateDao().getObjectsState();
    }

    @Override
    public void setMailboxes(
            final TypedState<Mailbox> mailboxTypedState, final Mailbox[] mailboxes) {
        final List<MailboxEntity> mailboxEntities = new ArrayList<>();
        for (Mailbox mailbox : mailboxes) {
            mailboxEntities.add(MailboxEntity.of(mailbox));
        }
        database.mailboxDao().set(mailboxEntities, mailboxTypedState.getState());
    }

    @Override
    public void updateMailboxes(final Update<Mailbox> update, final String[] updatedProperties)
            throws CacheWriteException, CacheConflictException {
        try {
            database.mailboxDao().update(update, updatedProperties);
        } catch (IllegalArgumentException e) {
            throw new CacheWriteException(e);
        }
    }

    @Override
    public Collection<? extends IdentifiableMailboxWithRole> getSpecialMailboxes()
            throws NotSynchronizedException {
        // TODO ensure that mailbox state exists?
        return database.mailboxDao().getSpecialMailboxes();
    }

    @Override
    public IdentifiableMailboxWithRoleAndName getMailboxByNameAndParent(
            String name, String parentId) throws NotSynchronizedException {
        if (parentId == null) {
            return database.mailboxDao().getMailboxByNameWhereParentIdIsNull(name);
        } else {
            return database.mailboxDao().getMailboxByNameAndParent(name, parentId);
        }
    }

    @Override
    public List<? extends IdentifiableMailboxWithRoleAndName> getMailboxesByNames(String[] names) {
        return database.mailboxDao().getMailboxesByNames(names);
    }

    @Override
    public void setThreadsAndEmails(
            final TypedState<Thread> threadState,
            final Thread[] threads,
            final TypedState<Email> emailState,
            final Email[] emails) {
        database.threadAndEmailDao().set(threadState, threads, emailState, emails);
    }

    @Override
    public void addThreadsAndEmail(
            final TypedState<Thread> threadState,
            final Thread[] threads,
            final TypedState<Email> emailState,
            final Email[] emails) {
        database.threadAndEmailDao().add(threadState, threads, emailState, emails);
    }

    @Override
    public void updateThreads(final Update<Thread> update) throws CacheWriteException {
        LOGGER.debug("updating threads {}", update);
        database.threadAndEmailDao().update(update);
    }

    @Override
    public void updateEmails(final Update<Email> update, final String[] updatedProperties)
            throws CacheWriteException {
        database.threadAndEmailDao().updateEmails(update, updatedProperties);
    }

    @Override
    public void invalidateEmailThreadsAndQueries() {
        database.stateDao().invalidateEmailThreadAndQueryStates();
    }

    @Override
    public void invalidateMailboxes() {
        database.stateDao().deleteState(Mailbox.class);
    }

    @Override
    public void setIdentities(
            final TypedState<Identity> identityTypedState, final Identity[] identities) {
        database.identityDao().set(identities, identityTypedState.getState());
    }

    @Override
    public void updateIdentities(final Update<Identity> update) throws CacheWriteException {
        LOGGER.debug("updating identities {}", update);
        database.identityDao().update(update);
    }

    @Override
    public void invalidateIdentities() {
        database.stateDao().deleteState(Identity.class);
    }

    @Override
    public void setQueryResult(final String queryString, final QueryResult queryResult) {
        database.queryDao().set(queryString, queryResult);
    }

    @Override
    public void addQueryResult(
            final String queryString, final String afterEmailId, final QueryResult queryResult)
            throws CacheConflictException {
        database.queryDao().add(queryString, afterEmailId, queryResult);
    }

    @Override
    public void updateQueryResults(
            final String queryString,
            final QueryUpdate<Email, QueryResultItem> queryUpdate,
            final TypedState<Email> emailTypedState)
            throws CacheConflictException {
        LOGGER.debug("updating query results {}", queryUpdate);
        database.queryDao().updateQueryResults(queryString, queryUpdate, emailTypedState);
    }

    @Override
    public void invalidateQueryResult(final String queryString) {
        database.stateDao().invalidateQueryState(queryString);
    }

    @Override
    public Missing getMissing(final String query) throws CacheReadException {
        final Missing missing = database.threadAndEmailDao().getMissing(query);
        LOGGER.debug("cache reported {} missing threads", missing.threadIds.size());
        return missing;
    }
}
