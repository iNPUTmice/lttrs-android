package rs.ltt.android.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rs.ltt.android.MuaPool;
import rs.ltt.android.database.AppDatabase;
import rs.ltt.android.entity.AccountWithCredentials;
import rs.ltt.android.entity.QueryInfo;
import rs.ltt.android.worker.QueryRefreshWorker;
import rs.ltt.jmap.client.event.OnStateChangeListener;
import rs.ltt.jmap.client.event.PushService;
import rs.ltt.jmap.common.entity.StateChange;
import rs.ltt.jmap.mua.Mua;

public class EventMonitorService extends Service {

    private static final String ACTION_WATCH_QUERY = "rs.ltt.android.ACTION_WATCH_QUERY";

    private static final String EXTRA_QUERY_INFO = "rs.ltt.android.EXTRA_QUERY_INFO";

    private static final Logger LOGGER = LoggerFactory.getLogger(EventMonitorService.class);

    static final Executor PUSH_SERVICE_BACKGROUND_EXECUTOR = Executors.newSingleThreadExecutor();

    private final Map<Long, EventMonitorRegistration> eventMonitorRegistrations = new HashMap<>();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        final ListenableFuture<List<AccountWithCredentials>> accounts = AppDatabase.getInstance(this)
                .accountDao()
                .getAccounts();
        Futures.addCallback(accounts, new FutureCallback<List<AccountWithCredentials>>() {
            @Override
            public void onSuccess(final List<AccountWithCredentials> accounts) {
                onAccountsLoaded(accounts);
            }

            @Override
            public void onFailure(@NonNull final Throwable throwable) {
                LOGGER.warn("Unable to load accounts from database", throwable);
            }
        }, PUSH_SERVICE_BACKGROUND_EXECUTOR);
    }

    @Override
    public void onDestroy() {
        LOGGER.info("Destroying service. Removing listeners");
        for (final EventMonitorRegistration registration : this.eventMonitorRegistrations.values()) {
            final PushService pushService = registration.pushService;
            pushService.removeOnStateChangeListener(registration.eventMonitor);
        }
    }

    private void onAccountsLoaded(final List<AccountWithCredentials> accounts) {
        LOGGER.info("{} accounts loaded", accounts.size());
        accounts.stream().forEach(this::setupEventMonitor);
    }

    private void setupEventMonitor(final AccountWithCredentials account) {
        final Mua mua = MuaPool.getInstance(this, account);
        final EventMonitor eventMonitor = new EventMonitor(account);
        ListenableFuture<PushService> pushServiceFuture = mua.getJmapClient().monitorEvents(eventMonitor);
        Futures.addCallback(pushServiceFuture, new FutureCallback<PushService>() {

            @Override
            public void onSuccess(@Nullable PushService result) {
                if (result == null) {
                    return;
                }
                final EventMonitorRegistration registration = new EventMonitorRegistration(result, eventMonitor);
                eventMonitorRegistrations.put(account.id, registration);
            }

            @Override
            public void onFailure(@NonNull final Throwable throwable) {
                LOGGER.warn("Unable to instantiate push service for account", throwable);

            }
        }, PUSH_SERVICE_BACKGROUND_EXECUTOR);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        final String action = intent == null ? null : intent.getAction();
        if (action == null) {
            return START_STICKY;
        }

        switch (action) {
            case ACTION_WATCH_QUERY:
                final QueryInfo queryInfo = intent.getParcelableExtra(EXTRA_QUERY_INFO);
                watchQuery(queryInfo);
                break;
        }


        //TODO: Add commands to start listening / stop listening to account. Used during setup
        //TODO: Add command for 'currently viewed query'
        return START_STICKY;
    }

    private void watchQuery(final QueryInfo queryInfo) {
        LOGGER.info("watchQuery({})", queryInfo);
        final WorkManager workManager = WorkManager.getInstance(getApplication());
        final OneTimeWorkRequest workRequest = QueryRefreshWorker.of(queryInfo, true);
        workManager.enqueueUniqueWork(
                QueryRefreshWorker.uniqueName(queryInfo.accountId),
                ExistingWorkPolicy.REPLACE,
                workRequest
        );
    }

    public static void watchQuery(final Context context, final QueryInfo queryInfo) {
        final Intent intent = new Intent(context, EventMonitorService.class);
        intent.setAction(ACTION_WATCH_QUERY);
        intent.putExtra(EXTRA_QUERY_INFO, queryInfo);
        context.startService(intent);
    }

    private static final class EventMonitorRegistration {
        private final PushService pushService;
        private final EventMonitor eventMonitor;

        private EventMonitorRegistration(PushService pushService, EventMonitor eventMonitor) {
            this.pushService = pushService;
            this.eventMonitor = eventMonitor;
        }
    }

    private class EventMonitor implements OnStateChangeListener {

        private final AccountWithCredentials account;

        public EventMonitor(final AccountWithCredentials account) {
            this.account = account;
        }

        @Override
        public boolean onStateChange(final StateChange stateChange) {
            LOGGER.info("Account {} received {}", account.getId(), stateChange);
            //TODO check that this is actually a new state
            return true;
        }
    }
}
