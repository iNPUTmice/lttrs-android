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
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkerParameters;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.entity.QueryInfo;
import rs.ltt.jmap.common.entity.query.EmailQuery;

public abstract class QueryRefreshWorker extends AbstractMuaWorker {

    protected static final String SKIP_OVER_EMPTY_KEY = "skipOverEmpty";
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryRefreshWorker.class);
    private final boolean skipOverEmpty;

    public QueryRefreshWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = workerParams.getInputData();
        this.skipOverEmpty = data.getBoolean(SKIP_OVER_EMPTY_KEY, false);
    }

    public static OneTimeWorkRequest main(long accountId) {
        return new OneTimeWorkRequest.Builder(MainMailboxQueryRefreshWorker.class)
                .setInputData(MainMailboxQueryRefreshWorker.data(accountId, false))
                .build();
    }

    public static OneTimeWorkRequest of(final QueryInfo queryInfo, final boolean skipOverEmpty) {
        switch (queryInfo.type) {
            case MAIN:
                return new OneTimeWorkRequest.Builder(MainMailboxQueryRefreshWorker.class)
                        .setInputData(
                                MainMailboxQueryRefreshWorker.data(
                                        queryInfo.accountId, skipOverEmpty))
                        .build();
            case MAILBOX:
                return new OneTimeWorkRequest.Builder(MailboxQueryRefreshWorker.class)
                        .setInputData(
                                MailboxQueryRefreshWorker.data(
                                        queryInfo.accountId, skipOverEmpty, queryInfo.value))
                        .build();
            case KEYWORD:
                return new OneTimeWorkRequest.Builder(KeywordQueryRefreshWorker.class)
                        .setInputData(
                                KeywordQueryRefreshWorker.data(
                                        queryInfo.accountId, skipOverEmpty, queryInfo.value))
                        .build();
            case SEARCH:
                return new OneTimeWorkRequest.Builder(SearchQueryRefreshWorker.class)
                        .setInputData(
                                SearchQueryRefreshWorker.data(
                                        queryInfo.accountId, skipOverEmpty, queryInfo.value))
                        .build();
            default:
                throw new IllegalArgumentException(
                        String.format("%s is an unknown Query Type", queryInfo.type));
        }
    }

    public static String uniqueName(final Long accountId) {
        return String.format(Locale.ENGLISH, "account-%d-query-refresh", accountId);
    }

    abstract EmailQuery getEmailQuery();

    @NonNull
    @Override
    public Result doWork() {
        try {
            final EmailQuery emailQuery = getEmailQuery();
            LOGGER.info("Refreshing {}", emailQuery);
            return refresh(emailQuery);
        } catch (final Exception e) {
            LOGGER.info("Unable to refresh query", e);
            return Result.failure();
        }
    }

    protected Result refresh(final EmailQuery emailQuery)
            throws ExecutionException, InterruptedException {
        throwOnEmpty(emailQuery);
        getMua().query(emailQuery).get();
        return Result.success();
    }

    protected void throwOnEmpty(final EmailQuery emailQuery) {
        if (skipOverEmpty && getDatabase().queryDao().empty(emailQuery.asHash())) {
            throw new IllegalStateException(
                    "Do not refresh because query is empty (UI will automatically load this)");
        }
    }
}
