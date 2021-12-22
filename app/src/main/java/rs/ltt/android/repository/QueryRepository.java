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

package rs.ltt.android.repository;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.entity.MailboxOverviewItem;
import rs.ltt.android.entity.MailboxWithRoleAndName;
import rs.ltt.android.entity.ThreadOverviewItem;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mua.Status;

public class QueryRepository extends AbstractMuaRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryRepository.class);

    private final Set<String> runningQueries = new HashSet<>();
    private final Set<String> runningPagingRequests = new HashSet<>();

    private final MutableLiveData<Set<String>> runningQueriesLiveData =
            new MutableLiveData<>(runningQueries);
    private final MutableLiveData<Set<String>> runningPagingRequestsLiveData =
            new MutableLiveData<>(runningPagingRequests);

    public QueryRepository(final Application application, final long accountId) {
        super(application, accountId);
    }

    public LiveData<PagedList<ThreadOverviewItem>> getThreadOverviewItems(final EmailQuery query) {
        return new LivePagedListBuilder<>(
                        database.queryDao().getThreadOverviewItems(query.asHash()), 30)
                .setBoundaryCallback(
                        new PagedList.BoundaryCallback<ThreadOverviewItem>() {
                            @Override
                            public void onZeroItemsLoaded() {
                                LOGGER.debug("onZeroItemsLoaded");
                                requestNextPage(
                                        query,
                                        null); // conceptually in terms of loading indicators this
                                // is more of a page request
                                super.onZeroItemsLoaded();
                            }

                            @Override
                            public void onItemAtEndLoaded(@NonNull ThreadOverviewItem itemAtEnd) {
                                LOGGER.debug("onItemAtEndLoaded({})", itemAtEnd.emailId);
                                requestNextPage(query, itemAtEnd.emailId);
                                super.onItemAtEndLoaded(itemAtEnd);
                            }
                        })
                .build();
    }

    public ListenableFuture<MailboxWithRoleAndName> getInbox() {
        return database.mailboxDao().getMailboxFuture(Role.INBOX);
    }

    public ListenableFuture<MailboxWithRoleAndName> getImportant() {
        return database.mailboxDao().getMailboxFuture(Role.IMPORTANT);
    }

    public LiveData<Boolean> isRunningQueryFor(final EmailQuery query) {
        return Transformations.map(
                runningQueriesLiveData, queryStrings -> queryStrings.contains(query.asHash()));
    }

    public LiveData<Boolean> isRunningPagingRequestFor(final EmailQuery query) {
        return Transformations.map(
                runningPagingRequestsLiveData,
                queryStrings -> queryStrings.contains(query.asHash()));
    }

    public void refresh(final EmailQuery emailQuery) {
        final String queryString = emailQuery.asHash();
        synchronized (this) {
            if (!runningQueries.add(queryString)) {
                LOGGER.debug("skipping refresh since already running");
                return;
            }
            if (runningPagingRequests.contains(queryString)) {
                // even though this refresh call is only implicit through the pageRequest we want to
                // display something nice for the user
                runningQueries.add(queryString);
                runningQueriesLiveData.postValue(runningQueries);
                LOGGER.debug("skipping refresh since we are running a page request");
                return;
            }
        }
        final ListenableFuture<Status> statusFuture =
                Futures.transformAsync(
                        mua, mua -> mua.query(emailQuery), MoreExecutors.directExecutor());
        statusFuture.addListener(
                () -> {
                    synchronized (runningQueries) {
                        runningQueries.remove(queryString);
                    }
                    runningQueriesLiveData.postValue(runningQueries);
                    try {
                        Status status = statusFuture.get();
                    } catch (Exception e) {
                        final Throwable cause = e.getCause();
                        LOGGER.debug("unable to refresh", cause);
                    }
                },
                MoreExecutors.directExecutor());
    }

    public boolean isRefreshing(final EmailQuery emailQuery) {
        final String queryString = emailQuery.asHash();
        synchronized (this) {
            return runningQueries.contains(queryString)
                    || runningPagingRequests.contains(queryString);
        }
    }

    private void requestNextPage(final EmailQuery emailQuery, String afterEmailId) {
        final String queryString = emailQuery.asHash();
        synchronized (this) {
            if (!runningPagingRequests.add(queryString)) {
                LOGGER.debug("skipping paging request since already running");
                return;
            }
            runningPagingRequestsLiveData.postValue(runningPagingRequests);
        }
        final ListenableFuture<Status> hadResults;
        if (afterEmailId == null) {
            hadResults =
                    Futures.transformAsync(
                            mua, mua -> mua.query(emailQuery), MoreExecutors.directExecutor());
        } else {
            hadResults =
                    Futures.transformAsync(
                            mua,
                            mua -> mua.query(emailQuery, afterEmailId),
                            MoreExecutors.directExecutor());
        }
        hadResults.addListener(
                () -> {
                    final boolean modifiedImplicitRefresh;
                    synchronized (this) {
                        runningPagingRequests.remove(queryString);
                        modifiedImplicitRefresh = runningQueries.remove(queryString);
                    }
                    runningPagingRequestsLiveData.postValue(runningPagingRequests);
                    if (modifiedImplicitRefresh) {
                        runningQueriesLiveData.postValue(runningQueries);
                    }
                    try {
                        LOGGER.debug("requestNextPageResult=" + hadResults.get());
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        LOGGER.debug("error retrieving the next page", cause);
                    } catch (Exception e) {
                        LOGGER.debug("error paging ", e);
                    }
                },
                MoreExecutors.directExecutor());
    }

    public LiveData<MailboxOverviewItem> getMailboxOverviewItem(final String mailboxId) {
        if (mailboxId == null) {
            return database.mailboxDao().getMailboxOverviewItemLiveData(Role.INBOX);
        } else {
            return database.mailboxDao().getMailboxOverviewItemLiveData(mailboxId);
        }
    }

    public LiveData<String[]> getTrashAndJunk() {
        return database.mailboxDao().getMailboxesLiveData(Role.TRASH, Role.JUNK);
    }
}
