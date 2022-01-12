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

package rs.ltt.android.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.database.LttrsDatabase;
import rs.ltt.android.entity.EmailWithMailboxes;
import rs.ltt.jmap.mua.Mua;

public class MoveToTrashWorker extends AbstractMuaWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoveToTrashWorker.class);

    private static final String THREAD_IDS_KEY = "threadIds";

    private final Collection<String> threadIds;

    public MoveToTrashWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = workerParams.getInputData();
        final String[] threadIds = data.getStringArray(THREAD_IDS_KEY);
        this.threadIds = threadIds != null ? Sets.newHashSet(threadIds) : Collections.emptySet();
    }

    public static Data data(Long account, Collection<String> threadIds) {
        return new Data.Builder()
                .putLong(ACCOUNT_KEY, account)
                .putStringArray(THREAD_IDS_KEY, threadIds.toArray(new String[0]))
                .build();
    }

    protected ListenableFuture<Boolean> modify(List<EmailWithMailboxes> emails)
            throws ExecutionException {
        final Mua mua = getMua();
        LOGGER.info("Modifying {} emails in threads {}", emails.size(), threadIds);
        return mua.moveToTrash(emails);
    }

    @NonNull
    @Override
    public Result doWork() {
        LttrsDatabase database = getDatabase();
        List<EmailWithMailboxes> emails =
                database.threadAndEmailDao().getEmailsWithMailboxes(threadIds);
        try {
            final boolean madeChanges = modify(emails).get();
            if (!madeChanges) {
                LOGGER.info("No changes were made to threads {}", threadIds);
                database.overwriteDao().revertMoveToTrashOverwrites(threadIds);
            }
            return Result.success();
        } catch (final ExecutionException e) {
            LOGGER.warn(String.format("Unable to modify emails in threads {}", threadIds), e);
            if (shouldRetry(e)) {
                return Result.retry();
            } else {
                database.overwriteDao().revertMoveToTrashOverwrites(threadIds);
                return Result.failure();
            }
        } catch (final InterruptedException e) {
            return Result.retry();
        }
    }
}
