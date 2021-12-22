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
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.database.LttrsDatabase;
import rs.ltt.android.entity.EmailWithKeywords;

public class DiscardDraftWorker extends AbstractMuaWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscardDraftWorker.class);

    private static final String DISCARD_ID_KEY = "discard";

    private final String emailId;

    public DiscardDraftWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = workerParams.getInputData();
        this.emailId = data.getString(DISCARD_ID_KEY);
    }

    public static Data data(Long account, String emailId) {
        return new Data.Builder()
                .putLong(ACCOUNT_KEY, account)
                .putString(DISCARD_ID_KEY, emailId)
                .build();
    }

    @NonNull
    @Override
    public Result doWork() {
        final LttrsDatabase database = getDatabase();
        EmailWithKeywords email = database.threadAndEmailDao().getEmailWithKeyword(this.emailId);
        try {
            final boolean madeChanges = getMua().discardDraft(email).get();
            if (madeChanges) {
                LOGGER.info("Discarded draft {}", email.getId());
            } else {
                LOGGER.warn("Unable to discard draft {}. No changes were made", email.getId());
            }
            return Result.success();
        } catch (ExecutionException e) {
            LOGGER.error("Unable to discard draft", e);
            if (shouldRetry(e)) {
                return Result.retry();
            } else {
                return Result.failure();
            }
        } catch (InterruptedException e) {
            return Result.retry();
        }
    }
}
