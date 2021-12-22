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

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.entity.QueryEntity;
import rs.ltt.android.entity.QueryItem;
import rs.ltt.android.entity.QueryItemEntity;
import rs.ltt.android.entity.ThreadOverviewItem;
import rs.ltt.jmap.common.entity.AddedItem;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.TypedState;
import rs.ltt.jmap.mua.cache.QueryUpdate;
import rs.ltt.jmap.mua.cache.exception.CacheConflictException;
import rs.ltt.jmap.mua.cache.exception.CorruptCacheException;
import rs.ltt.jmap.mua.util.QueryResult;
import rs.ltt.jmap.mua.util.QueryResultItem;

@Dao
public abstract class QueryDao extends AbstractEntityDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryDao.class);

    @Insert(onConflict = REPLACE)
    abstract long insert(QueryEntity entity);

    @Insert
    abstract void insert(List<QueryItemEntity> entities);

    @Insert
    abstract void insert(QueryItemEntity entity);

    @Query("delete from query_item_overwrite where executed=1 and queryId=:queryId")
    abstract int deleteAllExecuted(Long queryId);

    @Query("select * from `query` where queryString=:queryString and valid=1 limit 1")
    public abstract QueryEntity get(String queryString);

    @Query(
            "select position,emailId from query_item where queryId=:queryId order by position desc"
                    + " limit 1")
    abstract QueryItem getLastQueryItem(Long queryId);

    @Query("select count(id) from query_item where queryId=:queryId")
    abstract int getItemCount(Long queryId);

    @Query(
            "select case when exists(select query_item.id from `query` join query_item on"
                + " `query`.id = query_item.queryId where queryString=:queryString) then 0 else 1"
                + " end")
    public abstract boolean empty(String queryString);

    @Query("delete from `query` where queryString=:queryString")
    abstract void deleteQuery(String queryString);

    // we inner join on threads here to make sure that we only return items that we actually have
    // due to the delay of fetchMissing we might have query_items that we do not have a
    // corresponding thread for
    @Transaction
    @Query(
            "select query_item.threadId,query_item.emailId from `query` join query_item on"
                    + " `query`.id = query_item.queryId inner join thread on"
                    + " query_item.threadId=thread.threadId where queryString=:queryString  and "
                    + " query_item.threadId not in (select threadId from query_item_overwrite where"
                    + " queryId=`query`.id) order by position asc")
    public abstract DataSource.Factory<Integer, ThreadOverviewItem> getThreadOverviewItems(
            String queryString);

    @Query(
            "select query_item.emailId from `query` join query_item on `query`.id ="
                    + " query_item.queryId where queryString=:queryString order by position asc")
    public abstract List<String> getEmailIds(final String queryString);

    @Transaction
    public void set(String queryString, QueryResult queryResult) {
        TypedState<Email> emailState = queryResult.objectState;
        throwOnCacheConflict(Email.class, emailState);

        if (queryResult.items.length == 0) {
            deleteQuery(queryString);
            return;
        }

        long queryId =
                insert(
                        QueryEntity.of(
                                queryString,
                                queryResult.queryState.getState(),
                                queryResult.canCalculateChanges));
        insert(QueryItemEntity.of(queryId, queryResult.items, 0));
    }

    @Transaction
    public void add(String queryString, String afterEmailId, QueryResult queryResult) {

        final QueryEntity queryEntity = get(queryString);

        // TODO not having a state is fine; we still want to be able to page
        // TODO compare queryEntity.state only when it is not null
        if (queryEntity == null || queryEntity.state == null) {
            throw new CacheConflictException(
                    "Unable to append items to Query. Cached query state is unknown");
        }

        if (!queryEntity.state.equals(queryResult.queryState.getState())) {
            throw new CacheConflictException(
                    "Unable to append to Query. Cached query state did not meet our expectations");
        }

        TypedState<Email> emailState = queryResult.objectState;
        throwOnCacheConflict(Email.class, emailState);

        final QueryItem lastQueryItem = getLastQueryItem(queryEntity.id);

        if (!lastQueryItem.emailId.equals(afterEmailId)) {
            throw new CacheConflictException(
                    String.format(
                            "Current last email id in cache (%s) doesn't match afterId (%s) from"
                                    + " request",
                            lastQueryItem.emailId, afterEmailId));
        }

        if (lastQueryItem.position != queryResult.position - 1) {
            throw new CorruptCacheException(
                    String.format(
                            "Unexpected QueryPage. Cache ends with position %d. Page starts at"
                                    + " position %d",
                            lastQueryItem.position, queryResult.position));
        }

        if (queryResult.items.length > 0) {
            insert(QueryItemEntity.of(queryEntity.id, queryResult.items, queryResult.position));
        }
    }

    @Query("select * from `query` where queryString=:queryString")
    abstract QueryEntity getQueryEntity(String queryString);

    @Query(
            "update query_item set position=position+1 where queryId=:queryId and"
                    + " position>=:position ")
    abstract int incrementAllPositionsFrom(Long queryId, Long position);

    // TODO: is this query safe to run when emailId is not found
    @Query(
            "update query_item set position=position-1 where queryId=:queryId and position>(select"
                    + " position from query_item where emailId=:emailId and queryId=:queryId)")
    abstract void decrementAllPositionsFrom(Long queryId, String emailId);

    @Query("delete from query_item where queryId=:queryId and emailId=:emailId")
    abstract void deleteQueryItem(Long queryId, String emailId);

    @Query("update `query` set state=:newState where state=:oldState and id=:queryId")
    abstract int updateQueryState(Long queryId, String newState, String oldState);

    @Query("select state from `query` where queryString=:queryString")
    abstract String getQueryState(String queryString);

    @Transaction
    public void updateQueryResults(
            String queryString,
            QueryUpdate<Email, QueryResultItem> queryUpdate,
            final TypedState<Email> emailState) {
        final String newState = queryUpdate.getNewTypedState().getState();
        final String oldState = queryUpdate.getOldTypedState().getState();
        if (newState.equals(getQueryState(queryString))) {
            LOGGER.debug("nothing to do. query already at newest state");
            return;
        }
        throwOnCacheConflict(Email.class, emailState);
        final QueryEntity queryEntity = getQueryEntity(queryString);

        final int count = deleteAllExecuted(queryEntity.id);
        LOGGER.debug("deleted {} query overwrites", count);

        for (String emailId : queryUpdate.getRemoved()) {
            LOGGER.debug("deleting emailId=" + emailId + " from queryId=" + queryEntity.id);
            decrementAllPositionsFrom(queryEntity.id, emailId);
            deleteQueryItem(queryEntity.id, emailId);
        }
        for (AddedItem<QueryResultItem> addedItem : queryUpdate.getAdded()) {
            LOGGER.debug("adding item {}", addedItem);
            LOGGER.debug(
                    "increment all positions where queryId={} and position={}",
                    queryEntity.id,
                    addedItem.getIndex());

            if (incrementAllPositionsFrom(queryEntity.id, addedItem.getIndex()) == 0
                    && getItemCount(queryEntity.id) != addedItem.getIndex()) {
                LOGGER.debug("ignoring query item change at position = {}", addedItem.getIndex());
                continue;
            }
            LOGGER.debug(
                    "insert queryItemEntity on position {} and id={}",
                    addedItem.getIndex(),
                    queryEntity.id);
            insert(QueryItemEntity.of(queryEntity.id, addedItem.getIndex(), addedItem.getItem()));
        }

        if (updateQueryState(queryEntity.id, newState, oldState) != 1) {
            throw new CacheConflictException(
                    "Unable to update query from oldState={}"
                            + oldState
                            + " to newState="
                            + newState);
        }
    }
}
