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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rs.ltt.jmap.common.entity.query.EmailQuery;

public abstract class AbstractQueryRefreshWorker extends AbstractMuaWorker {

    protected static final String SKIP_OVER_EMPTY_KEY = "skipOverEmpty";
    protected final boolean skipOverEmpty;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractQueryRefreshWorker.class);

    public AbstractQueryRefreshWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = workerParams.getInputData();
        this.skipOverEmpty = data.getBoolean(SKIP_OVER_EMPTY_KEY, false);
    }

    abstract EmailQuery getEmailQuery();

    @NonNull
    @Override
    public Result doWork() {
        final EmailQuery emailQuery = getEmailQuery();
        if (skipOverEmpty && getDatabase().queryDao().empty(emailQuery.asHash())) {
            LOGGER.warn("Do not refresh because query is empty (UI will automatically load this)");
            return Result.failure();
        }
        try {
            getMua().query(getEmailQuery()).get();
            return Result.success();
        } catch (Exception e) {
            LOGGER.info("Unable to refresh query", e);
            return Result.failure();
        }
    }
}
