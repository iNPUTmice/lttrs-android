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

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import rs.ltt.android.entity.EntityStateEntity;
import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.Thread;
import rs.ltt.jmap.mua.cache.ObjectsState;
import rs.ltt.jmap.mua.cache.QueryStateWrapper;

@Dao
public abstract class StateDao {

    @Query("select state,type from entity_state where type in (:types)")
    public abstract List<EntityStateEntity> getEntityStates(
            List<Class<? extends AbstractIdentifiableEntity>> types);

    public ObjectsState getObjectsState() {
        final List<EntityStateEntity> entityStates =
                getEntityStates(Arrays.asList(Email.class, Mailbox.class, Thread.class));
        final ObjectsState.Builder builder = ObjectsState.builder();
        for (final EntityStateEntity entityState : entityStates) {
            if (entityState.type == Mailbox.class) {
                builder.setMailboxState(entityState.state);
            } else if (entityState.type == Thread.class) {
                builder.setThreadState(entityState.state);
            } else if (entityState.type == Email.class) {
                builder.setEmailState(entityState.state);
            } else {
                throw new IllegalStateException("Database returned state that we can not process");
            }
        }
        return builder.build();
    }

    @Query(
            "select state,canCalculateChanges from `query` where queryString=:queryString and"
                    + " valid=1")
    abstract QueryState getQueryState(String queryString);

    @Query(
            "select emailId as id,position from `query` join query_item on `query`.id = queryId "
                    + " where queryString=:queryString order by position desc limit 1")
    abstract QueryStateWrapper.UpTo getUpTo(String queryString);

    @Query("update `query` set valid=0 where queryString=:queryString")
    public abstract void invalidateQueryState(String queryString);

    @Query("update `query` set valid=0")
    abstract void invalidateQueryStates();

    @Query("delete from entity_state where type=:entityType")
    public abstract void deleteState(Class<? extends AbstractIdentifiableEntity> entityType);

    @Query("delete from entity_state where type in(:entityTypes)")
    abstract void deleteStates(List<Class<? extends AbstractIdentifiableEntity>> entityTypes);

    @Transaction
    public void invalidateEmailThreadAndQueryStates() {
        deleteStates(ImmutableList.of(Email.class, Thread.class));
        invalidateQueryStates();
    }

    @Transaction
    public QueryStateWrapper getQueryStateWrapper(String queryString) {
        final QueryState queryState = getQueryState(queryString);
        final ObjectsState objectsState = getObjectsState();
        // TODO we maybe want to include upTo even if no queryState was found to make cache
        // invalidation more graceful
        final QueryStateWrapper.UpTo upTo;
        if (queryState == null) {
            return new QueryStateWrapper(null, false, null, objectsState);
        } else {
            upTo = getUpTo(queryString);
            return new QueryStateWrapper(
                    queryState.state, queryState.canCalculateChanges, upTo, objectsState);
        }
    }

    public static class QueryState {
        public String state;
        public Boolean canCalculateChanges;
    }
}
