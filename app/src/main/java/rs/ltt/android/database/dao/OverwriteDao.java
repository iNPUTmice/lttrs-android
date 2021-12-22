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

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.entity.KeywordOverwriteEntity;
import rs.ltt.android.entity.MailboxOverwriteEntity;
import rs.ltt.android.entity.QueryItemOverwriteEntity;

@Dao
public abstract class OverwriteDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverwriteDao.class);

    @Insert(onConflict = REPLACE)
    public abstract void insertKeywordOverwrites(
            Collection<KeywordOverwriteEntity> keywordOverwriteEntities);

    @Insert(onConflict = REPLACE)
    public abstract void insertMailboxOverwrites(
            Collection<MailboxOverwriteEntity> mailboxOverwriteEntities);

    @Insert(onConflict = REPLACE)
    public abstract void insertQueryOverwrites(
            Collection<QueryItemOverwriteEntity> queryItemOverwriteEntities);

    @Delete
    public abstract void deleteQueryOverwrites(
            Collection<QueryItemOverwriteEntity> queryItemOverwriteEntity);

    @Query("delete from query_item_overwrite where threadId=:threadId and type=:type")
    public abstract int deleteQueryOverwritesByThread(
            String threadId, QueryItemOverwriteEntity.Type type);

    @Query("delete from query_item_overwrite where threadId in (:threadIds)")
    public abstract int deleteQueryOverwritesByThread(Collection<String> threadIds);

    @Query("delete from mailbox_overwrite where threadId=:threadId")
    protected abstract int deleteMailboxOverwritesByThread(String threadId);

    @Query("delete from mailbox_overwrite where threadId in (:threadIds)")
    protected abstract int deleteMailboxOverwritesByThread(Collection<String> threadIds);

    @Query("delete from keyword_overwrite where threadId=:threadId")
    protected abstract int deleteKeywordOverwritesByThread(String threadId);

    @Transaction
    public void revertKeywordOverwrites(final String threadId) {
        final int keywordOverwrites = deleteKeywordOverwritesByThread(threadId);
        final int queryOverwrites =
                deleteQueryOverwritesByThread(threadId, QueryItemOverwriteEntity.Type.KEYWORD);
        if (keywordOverwrites > 0 || queryOverwrites > 0) {
            LOGGER.info(
                    "Deleted {} keyword overwrites and {} query overwrites for thread {}",
                    keywordOverwrites,
                    queryOverwrites,
                    threadId);
        }
    }

    @Transaction
    public void revertMailboxOverwrites(final String threadId) {
        final int mailboxOverwrites = deleteMailboxOverwritesByThread(threadId);
        final int queryOverwrites =
                deleteQueryOverwritesByThread(threadId, QueryItemOverwriteEntity.Type.MAILBOX);
        if (mailboxOverwrites > 0 || queryOverwrites > 0) {
            LOGGER.info(
                    "Deleted {} mailbox overwrites and {} query overwrites for thread {}",
                    mailboxOverwrites,
                    queryOverwrites,
                    threadId);
        }
    }

    @Transaction
    public void revertMoveToTrashOverwrites(final Collection<String> threadIds) {
        final int mailboxOverwrites = deleteMailboxOverwritesByThread(threadIds);
        final int queryOverwrites = deleteQueryOverwritesByThread(threadIds);
        if (mailboxOverwrites > 0 || queryOverwrites > 0) {
            LOGGER.info(
                    "Deleted {} mailbox overwrites and {} query overwrites for threads {}",
                    mailboxOverwrites,
                    queryOverwrites,
                    threadIds);
        }
    }

    @Query("select * from keyword_overwrite where threadId=:threadId")
    public abstract ListenableFuture<KeywordOverwriteEntity> getKeywordOverwrite(String threadId);

    @Query("select * from mailbox_overwrite where threadId=:threadId")
    public abstract LiveData<List<MailboxOverwriteEntity>> getMailboxOverwrites(String threadId);

    @Query("select * from mailbox_overwrite where threadId in(:threadIds)")
    public abstract LiveData<List<MailboxOverwriteEntity>> getMailboxOverwrites(String[] threadIds);
}
