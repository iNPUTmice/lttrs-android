package rs.ltt.android.service;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import rs.ltt.android.BuildConfig;
import rs.ltt.android.MuaPool;
import rs.ltt.android.database.AppDatabase;
import rs.ltt.android.entity.AccountWithCredentials;
import rs.ltt.android.entity.QueryInfo;
import rs.ltt.android.ui.notification.EmailNotification;
import rs.ltt.android.ui.notification.ForegroundServiceNotification;
import rs.ltt.android.worker.QueryRefreshWorker;
import rs.ltt.jmap.client.event.OnConnectionStateChangeListener;
import rs.ltt.jmap.client.event.OnStateChangeListener;
import rs.ltt.jmap.client.event.PushService;
import rs.ltt.jmap.client.event.State;
import rs.ltt.jmap.common.entity.StateChange;
import rs.ltt.jmap.mua.Mua;

public class EventMonitorService extends LifecycleService {

    static final Executor PUSH_SERVICE_BACKGROUND_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final String ACTION_WATCH_QUERY = "rs.ltt.android.ACTION_WATCH_QUERY";
    private static final String ACTION_START_MONITORING = "rs.ltt.android.ACTION_START_MONITORING";
    private static final String ACTION_STOP_MONITORING = "rs.ltt.android.ACTION_STOP_MONITORING";
    private static final String EXTRA_ACCOUNT_ID = "rs.ltt.android.EXTRA_ACCOUNT_ID";
    private static final String EXTRA_QUERY_INFO = "rs.ltt.android.EXTRA_QUERY_INFO";
    private static final Logger LOGGER = LoggerFactory.getLogger(EventMonitorService.class);
    private final Map<Long, EventMonitorRegistration> eventMonitorRegistrations = new HashMap<>();
    private QueryInfo currentlyWatchedQuery = null;

    public static void watchQuery(final Context context, final QueryInfo queryInfo) {
        final Intent intent = new Intent(context, EventMonitorService.class);
        intent.setAction(ACTION_WATCH_QUERY);
        intent.putExtra(EXTRA_QUERY_INFO, queryInfo);
        context.startService(intent);
    }

    public static void startMonitoring(final Context context, final Collection<Long> accountIds) {
        for (final long accountId : accountIds) {
            startMonitoring(context, accountId);
        }
    }

    public static void startMonitoring(final Context context, final long accountId) {
        final Intent intent = new Intent(context, EventMonitorService.class);
        intent.setAction(ACTION_START_MONITORING);
        intent.putExtra(EXTRA_ACCOUNT_ID, accountId);
        context.startService(intent);
    }

    public static void stopMonitoring(final Context context, final long accountId) {
        final Intent intent = new Intent(context, EventMonitorService.class);
        intent.setAction(ACTION_STOP_MONITORING);
        intent.putExtra(EXTRA_ACCOUNT_ID, accountId);
        context.startService(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(@NotNull final Intent intent) {
        super.onBind(intent);
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
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

        if (BuildConfig.USE_FOREGROUND_SERVICE) {
            startForeground(
                    ForegroundServiceNotification.ID,
                    ForegroundServiceNotification.get(this)
            );
        }
    }

    @Override
    public void onDestroy() {
        LOGGER.debug("Destroying service. Removing listeners");
        synchronized (this.eventMonitorRegistrations) {
            final Iterator<EventMonitorRegistration> iterator = this.eventMonitorRegistrations.values().iterator();
            while (iterator.hasNext()) {
                final EventMonitorRegistration registration = iterator.next();
                registration.stopListening();
                iterator.remove();
            }
        }
        super.onDestroy();
    }

    private void onAccountsLoaded(final List<AccountWithCredentials> accounts) {
        final Lifecycle.State state = getLifecycle().getCurrentState();
        if (state.isAtLeast(Lifecycle.State.INITIALIZED)) {
            LOGGER.debug("{} accounts loaded while in state {}", accounts.size(), state);
            accounts.stream().forEach(this::setup);
        }
    }

    private void onAccountLoaded(final AccountWithCredentials account) {
        final Lifecycle.State state = getLifecycle().getCurrentState();
        if (state.isAtLeast(Lifecycle.State.INITIALIZED)) {
            setup(account);
        }
    }

    private void startMonitoring(final long accountId) {
        final ListenableFuture<AccountWithCredentials> accountFuture = AppDatabase.getInstance(this)
                .accountDao()
                .getAccountFuture(accountId);
        Futures.addCallback(accountFuture, new FutureCallback<AccountWithCredentials>() {
            @Override
            public void onSuccess(final AccountWithCredentials account) {
                if (account == null) {
                    return;
                }
                onAccountLoaded(account);
            }

            @Override
            public void onFailure(@NotNull final Throwable throwable) {
                LOGGER.warn("Unable to load account from database", throwable);
            }
        }, PUSH_SERVICE_BACKGROUND_EXECUTOR);
    }

    private void stopMonitoring(final long accountId) {
        synchronized (this.eventMonitorRegistrations) {
            final EventMonitorRegistration registration = this.eventMonitorRegistrations.remove(accountId);
            if (registration != null) {
                registration.stopListening();
            }
        }
        onConnectionStateChange();
    }

    private void setup(final AccountWithCredentials account) {
        EmailNotification.createChannel(getApplicationContext(), account);
        setupEventMonitor(account);
    }

    private void setupEventMonitor(final AccountWithCredentials account) {
        final EventMonitor eventMonitor;
        final ListenableFuture<PushService> pushServiceFuture;
        synchronized (eventMonitorRegistrations) {
            if (eventMonitorRegistrations.containsKey(account.getId())) {
                return;
            }
            final Mua mua = MuaPool.getInstance(this, account);
            eventMonitor = new EventMonitor(account);
            eventMonitorRegistrations.put(account.getId(), new EventMonitorRegistration(eventMonitor));
            pushServiceFuture = mua.getJmapClient().monitorEvents();
        }
        Futures.addCallback(pushServiceFuture, new FutureCallback<PushService>() {

            @Override
            public void onSuccess(@Nullable PushService pushService) {
                if (pushService == null) {
                    return;
                }
                final Lifecycle.State currentState = getLifecycle().getCurrentState();
                if (currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
                    final EventMonitorRegistration registration = new EventMonitorRegistration(
                            pushService,
                            eventMonitor
                    );
                    pushService.addOnStateChangeListener(eventMonitor);
                    pushService.addOnConnectionStateListener(eventMonitor);
                    synchronized (eventMonitorRegistrations) {
                        eventMonitorRegistrations.put(account.getId(), registration);
                    }
                    onConnectionStateChange();
                } else {
                    LOGGER.debug("Not going to listen for StateChanges. Service is {}", currentState);
                }
            }

            @Override
            public void onFailure(@NonNull final Throwable throwable) {
                LOGGER.warn("Unable to instantiate push service", throwable);

            }
        }, PUSH_SERVICE_BACKGROUND_EXECUTOR);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        super.onStartCommand(intent, flags, startId);
        final String action = Strings.nullToEmpty(intent == null ? null : intent.getAction());
        switch (action) {
            case ACTION_WATCH_QUERY:
                final QueryInfo queryInfo = Objects.requireNonNull(intent)
                        .getParcelableExtra(EXTRA_QUERY_INFO);
                watchQuery(queryInfo);
                break;
            case ACTION_START_MONITORING:
                startMonitoring(
                        Objects.requireNonNull(intent).getLongExtra(EXTRA_ACCOUNT_ID, -1)
                );
                break;
            case ACTION_STOP_MONITORING:
                stopMonitoring(
                        Objects.requireNonNull(intent).getLongExtra(EXTRA_ACCOUNT_ID, -1)
                );
                break;
            default:
                LOGGER.warn("Unknown action {}", action);
                break;
        }
        return START_STICKY;
    }

    private void watchQuery(final QueryInfo queryInfo) {
        LOGGER.debug("watchQuery({})", queryInfo);
        this.currentlyWatchedQuery = queryInfo;
        final WorkManager workManager = WorkManager.getInstance(getApplication());
        final OneTimeWorkRequest workRequest = QueryRefreshWorker.of(queryInfo, true);
        workManager.enqueueUniqueWork(
                QueryRefreshWorker.uniqueName(queryInfo.accountId),
                ExistingWorkPolicy.REPLACE,
                workRequest
        );
    }

    private boolean onStateChange(final AccountWithCredentials account, final StateChange stateChange) {
        final boolean activityStarted = ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
        LOGGER.debug("Account {} received {}", account.getId(), stateChange);
        final QueryInfo queryInfo = this.currentlyWatchedQuery;
        final OneTimeWorkRequest workRequest;
        if (activityStarted && queryInfo != null && queryInfo.accountId == account.getId()) {
            LOGGER.debug("Refreshing {} ", queryInfo);
            workRequest = QueryRefreshWorker.of(queryInfo, false);
        } else {
            LOGGER.debug("Refreshing MainMailbox");
            workRequest = QueryRefreshWorker.main(account.getId());
        }
        final WorkManager workManager = WorkManager.getInstance(getApplication());
        workManager.enqueueUniqueWork(
                QueryRefreshWorker.uniqueName(queryInfo.accountId),
                ExistingWorkPolicy.REPLACE,
                workRequest
        );
        return true;
    }

    private void onConnectionStateChange() {
        if (BuildConfig.USE_FOREGROUND_SERVICE) {
            updateNotification();
        }
    }

    private void updateNotification() {
        ForegroundServiceNotification.updateConnectionState(this, getCombinedState());
    }

    private State getCombinedState() {
        synchronized (eventMonitorRegistrations) {
            final Collection<EventMonitorRegistration> eventMonitors = eventMonitorRegistrations.values();
            return State.reduce(eventMonitors.stream()
                    .map(EventMonitorRegistration::getConnectionState)
                    .collect(Collectors.toList())
            );
        }
    }

    private static final class EventMonitorRegistration {
        private final PushService pushService;
        private final EventMonitor eventMonitor;

        private EventMonitorRegistration(final EventMonitor eventMonitor) {
            this.pushService = null;
            this.eventMonitor = eventMonitor;
        }

        private EventMonitorRegistration(PushService pushService, EventMonitor eventMonitor) {
            this.pushService = pushService;
            this.eventMonitor = eventMonitor;
        }

        public void stopListening() {
            if (this.pushService != null) {
                this.pushService.removeOnStateChangeListener(this.eventMonitor);
                this.pushService.removeOnConnectionStateListener(this.eventMonitor);
            }
        }

        public State getConnectionState() {
            return pushService == null ? State.CLOSED : pushService.getConnectionState();
        }
    }

    private class EventMonitor implements OnStateChangeListener, OnConnectionStateChangeListener {

        private final AccountWithCredentials account;

        public EventMonitor(final AccountWithCredentials account) {
            this.account = account;
        }

        @Override
        public boolean onStateChange(final StateChange stateChange) {
            return EventMonitorService.this.onStateChange(account, stateChange);
        }

        @Override
        public void onConnectionStateChange(final State state) {
            EventMonitorService.this.onConnectionStateChange();
        }
    }

}
