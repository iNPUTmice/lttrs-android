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
import android.database.sqlite.SQLiteDatabase;

import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.common.collect.Collections2;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import okhttp3.HttpUrl;
import rs.ltt.android.BuildConfig;
import rs.ltt.android.LttrsApplication;
import rs.ltt.android.MuaPool;
import rs.ltt.android.database.AppDatabase;
import rs.ltt.android.database.LttrsDatabase;
import rs.ltt.android.entity.AccountName;
import rs.ltt.android.entity.AccountWithCredentials;
import rs.ltt.android.entity.SearchSuggestionEntity;
import rs.ltt.android.push.PushManager;
import rs.ltt.android.service.EventMonitorService;
import rs.ltt.android.ui.notification.EmailNotification;
import rs.ltt.android.worker.AbstractMuaWorker;
import rs.ltt.android.worker.MainMailboxQueryRefreshWorker;
import rs.ltt.android.worker.QueryRefreshWorker;
import rs.ltt.jmap.common.entity.Account;
import rs.ltt.jmap.mua.Mua;
import rs.ltt.jmap.mua.Status;

public class MainRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainRepository.class);

    private static final ListeningExecutorService IO_EXECUTOR = MoreExecutors.listeningDecorator(
            Executors.newSingleThreadExecutor()
    );

    private final AppDatabase appDatabase;
    private final Application application;

    private ListenableFuture<?> networkFuture = null;

    public MainRepository(final Application application) {
        this.application = application;
        this.appDatabase = AppDatabase.getInstance(application);
    }

    public void insertSearchSuggestion(final String term) {
        IO_EXECUTOR.execute(() -> appDatabase.searchSuggestionDao().insert(SearchSuggestionEntity.of(term)));
    }

    public ListenableFuture<Long> insertAccountsRefreshMailboxes(final String username,
                                                                 final String password,
                                                                 final HttpUrl sessionResource,
                                                                 final String primaryAccountId,
                                                                 final Map<String, Account> accounts) {
        return Futures.transformAsync(insert(username, password, sessionResource, accounts), credentials -> {
            final Map<String, Long> accountIdMap = credentials.stream()
                    .collect(Collectors.toMap(
                            AccountWithCredentials::getAccountId,
                            AccountWithCredentials::getId
                    ));

            EventMonitorService.startMonitoring(application, accountIdMap.values());

            if (PushManager.register(application, credentials)) {
                LOGGER.info("Attempting to register for Firebase Messaging");
            } else {
                LOGGER.info("Firebase Messaging (Push) is not available in flavor {}", BuildConfig.FLAVOR);
                scheduleRecurringMainQueryWorkers(accountIdMap.values());
            }

            final Long internalIdForPrimary = accountIdMap.getOrDefault(
                    primaryAccountId,
                    accountIdMap.values().stream().findFirst().get()
            );
            final Collection<ListenableFuture<Status>> mailboxRefreshes = Collections2.transform(
                    credentials,
                    this::retrieveMailboxes
            );
            final ListenableFuture<Long> future = Futures.whenAllComplete(mailboxRefreshes).call(
                    () -> internalIdForPrimary,
                    MoreExecutors.directExecutor()
            );
            this.networkFuture = future;
            return future;
        }, IO_EXECUTOR);
    }

    private void scheduleRecurringMainQueryWorkers(final Collection<Long> accountIds) {
        final WorkManager workManager = WorkManager.getInstance(application);
        for (final Long accountId : accountIds) {
            final PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(MainMailboxQueryRefreshWorker.class, 15, TimeUnit.MINUTES, 20, TimeUnit.MINUTES)
                    .setInputData(MainMailboxQueryRefreshWorker.data(accountId, true))
                    .setConstraints(new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                    .build();
            workManager.enqueueUniquePeriodicWork(
                    MainMailboxQueryRefreshWorker.uniquePeriodicName(accountId),
                    ExistingPeriodicWorkPolicy.REPLACE,
                    periodicWorkRequest
            );
        }
    }

    private ListenableFuture<List<AccountWithCredentials>> insert(final String username,
                                                                  final String password,
                                                                  final HttpUrl sessionResource,
                                                                  final Map<String, Account> accounts) {
        return IO_EXECUTOR.submit(() -> appDatabase.accountDao().insert(
                username,
                password,
                sessionResource,
                accounts
        ));
    }

    private ListenableFuture<Status> retrieveMailboxes(final AccountWithCredentials account) {
        final Mua mua = MuaPool.getInstance(application, account);
        mua.refreshIdentities();
        return mua.refreshMailboxes();
    }


    public LiveData<AccountName> getAccountName(final Long id) {
        return this.appDatabase.accountDao().getAccountNameLiveData(id);
    }

    public LiveData<List<AccountName>> getAccountNames() {
        return this.appDatabase.accountDao().getAccountNames();
    }

    public void setSelectedAccount(final Long id) {
        LOGGER.debug("setSelectedAccount({})", id);
        IO_EXECUTOR.execute(() -> this.appDatabase.accountDao().selectAccount(id));
    }

    public ListenableFuture<Void> removeAccountAsync(final long accountId) {
        return IO_EXECUTOR.submit(() -> {
            removeAccount(accountId);
            return null;
        });
    }

    private void removeAccount(final long accountId) {
        this.appDatabase.accountDao().delete(accountId);
        LttrsApplication.get(application).invalidateMostRecentlySelectedAccountId();
        EventMonitorService.stopMonitoring(application, accountId);
        cancelAllWork(accountId);
        MuaPool.evict(accountId);
        final File file = LttrsDatabase.close(accountId);
        if (file != null && SQLiteDatabase.deleteDatabase(file)) {
            LOGGER.debug("Successfully deleted {}", file.getAbsolutePath());
        }
        EmailNotification.cancel(application, accountId);
        EmailNotification.deleteChannel(application, accountId);
    }

    public boolean cancelNetworkFuture() {
        final ListenableFuture<?> currentNetworkFuture = this.networkFuture;
        if (currentNetworkFuture == null || currentNetworkFuture.isDone()) {
            return false;
        }
        return currentNetworkFuture.cancel(true);
    }

    private void cancelAllWork(final Long accountId) {
        final WorkManager workManager = WorkManager.getInstance(application);
        workManager.cancelUniqueWork(AbstractMuaWorker.uniqueName(accountId));
        workManager.cancelUniqueWork(QueryRefreshWorker.uniqueName(accountId));
        workManager.cancelUniqueWork(MainMailboxQueryRefreshWorker.uniquePeriodicName(accountId));
    }
}
