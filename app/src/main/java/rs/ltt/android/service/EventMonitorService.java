package rs.ltt.android.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rs.ltt.android.MuaPool;
import rs.ltt.android.database.AppDatabase;
import rs.ltt.android.entity.AccountWithCredentials;
import rs.ltt.jmap.client.event.OnStateChangeListener;
import rs.ltt.jmap.client.event.PushService;
import rs.ltt.jmap.common.entity.StateChange;
import rs.ltt.jmap.mua.Mua;

public class EventMonitorService extends Service {

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
        ListenableFuture<rs.ltt.jmap.client.event.PushService> pushServiceFuture = mua.getJmapClient().monitorEvents(eventMonitor);
        Futures.addCallback(pushServiceFuture, new FutureCallback<rs.ltt.jmap.client.event.PushService>() {

            @Override
            public void onSuccess(@Nullable rs.ltt.jmap.client.event.PushService result) {
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
        return START_STICKY;
    }


    private static final class EventMonitorRegistration {
        private final rs.ltt.jmap.client.event.PushService pushService;
        private final EventMonitor eventMonitor;

        private EventMonitorRegistration(rs.ltt.jmap.client.event.PushService pushService, EventMonitor eventMonitor) {
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
            return false;
        }
    }
}
