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
import rs.ltt.android.entity.IdentityWithNameAndEmail;

public class SubmitEmailWorker extends AbstractMuaWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmitEmailWorker.class);

    private static final String EMAIL_ID = "emailId";
    private static final String IDENTITY_KEY = "identity";

    private final String emailId;
    private final String identity;

    public SubmitEmailWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = workerParams.getInputData();
        this.emailId = data.getString(EMAIL_ID);
        this.identity = data.getString(IDENTITY_KEY);
    }

    public static Data data(Long account, String identity, String emailId) {
        return new Data.Builder()
                .putLong(ACCOUNT_KEY, account)
                .putString(IDENTITY_KEY, identity)
                .putString(EMAIL_ID, emailId)
                .build();
    }

    @NonNull
    @Override
    public Result doWork() {
        IdentityWithNameAndEmail identity =
                getDatabase().identityDao().get(this.account, this.identity);
        try {
            final boolean madeChanges = getMua().submit(this.emailId, identity).get();
            if (madeChanges) {
                LOGGER.info("Submitted draft {}", this.emailId);
                getMua().refresh().get();
            } else {
                LOGGER.info("Unable to submit {}. No changes were made", this.emailId);
            }
            return Result.success();
        } catch (ExecutionException e) {
            LOGGER.warn("Unable to submit draft", e);
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
