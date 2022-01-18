package rs.ltt.android.push;

import android.content.Context;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.damnhandy.uri.template.UriTemplate;
import com.google.android.gms.common.GoogleApiAvailabilityLight;
import com.google.android.gms.tasks.Task;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.R;
import rs.ltt.android.database.AppDatabase;
import rs.ltt.android.entity.AccountWithCredentials;
import rs.ltt.android.entity.CredentialsEntity;
import rs.ltt.android.util.JmapClients;
import rs.ltt.android.worker.QueryRefreshWorker;
import rs.ltt.jmap.client.JmapClient;
import rs.ltt.jmap.client.MethodResponses;
import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.common.entity.PushMessage;
import rs.ltt.jmap.common.entity.PushSubscription;
import rs.ltt.jmap.common.entity.PushVerification;
import rs.ltt.jmap.common.entity.StateChange;
import rs.ltt.jmap.common.method.call.core.SetPushSubscriptionMethodCall;
import rs.ltt.jmap.common.method.response.core.SetPushSubscriptionMethodResponse;

public class PushManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushManager.class);

    private final Context context;

    public PushManager(final Context context) {
        this.context = context;
    }

    public void onMessageReceived(final long cid, final PushMessage pushMessage) {
        if (pushMessage instanceof PushVerification) {
            onPushVerificationReceived(cid, (PushVerification) pushMessage);
        } else if (pushMessage instanceof StateChange) {
            onStateChangeReceived(cid, (StateChange) pushMessage);
        } else {
            throw new IllegalArgumentException(
                    String.format(
                            "Receiving %s messages not implemented",
                            pushMessage.getClass().getSimpleName()));
        }
    }

    private void onPushVerificationReceived(final long cid, final PushVerification pushMessage) {
        LOGGER.info("onPushVerificationReceived({},{})", cid, pushMessage);
        final CredentialsEntity credentials =
                AppDatabase.getInstance(context).accountDao().getCredentials(cid);
        onPushVerificationReceived(credentials, pushMessage);
    }

    private void onPushVerificationReceived(
            final CredentialsEntity credentials, final PushVerification pushMessage) {
        final JmapClient jmapClient = JmapClients.of(context, credentials);
        final SetPushSubscriptionMethodCall setPushSubscription =
                SetPushSubscriptionMethodCall.builder()
                        .update(
                                ImmutableMap.of(
                                        pushMessage.getPushSubscriptionId(),
                                        ImmutableMap.of(
                                                "verificationCode",
                                                pushMessage.getVerificationCode())))
                        .build();
        final ListenableFuture<MethodResponses> methodResponsesFuture =
                jmapClient.call(setPushSubscription);
        Futures.addCallback(
                methodResponsesFuture,
                new FutureCallback<MethodResponses>() {
                    @Override
                    public void onSuccess(@Nullable MethodResponses methodResponses) {
                        final SetPushSubscriptionMethodResponse setResponse =
                                methodResponses.getMain(SetPushSubscriptionMethodResponse.class);
                        if (setResponse.getUpdated().size() == 1) {
                            LOGGER.info("Successfully set verification code");
                        } else {
                            LOGGER.error("Unable to set verification code. No updates?");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull final Throwable throwable) {
                        LOGGER.warn("Unable to set verification code", throwable);
                    }
                },
                MoreExecutors.directExecutor());
    }

    private void onStateChangeReceived(final long cid, final StateChange pushMessage) {
        LOGGER.info("onStateChangeReceived({},{})", cid, pushMessage);
        for (final Map.Entry<String, Map<Class<? extends AbstractIdentifiableEntity>, String>>
                entry : pushMessage.getChanged().entrySet()) {
            final String accountId = entry.getKey();
            final Map<Class<? extends AbstractIdentifiableEntity>, String> change =
                    entry.getValue();
            final AccountWithCredentials account =
                    AppDatabase.getInstance(context).accountDao().getAccount(cid, accountId);
            if (account == null) {
                LOGGER.error("Account with cid={} and accountId={} not found", cid, accountId);
                continue;
            }
            onStateChangeReceived(account, change);
        }
    }

    private void onStateChangeReceived(
            final AccountWithCredentials account,
            final Map<Class<? extends AbstractIdentifiableEntity>, String> change) {
        final boolean activityStarted =
                ProcessLifecycleOwner.get()
                        .getLifecycle()
                        .getCurrentState()
                        .isAtLeast(Lifecycle.State.STARTED);
        LOGGER.error(
                "Account {} has received a state change {} (activityStarted={})",
                account.getName(),
                change,
                activityStarted);
        // TODO skip if application is in foreground (it's just easier to test if we don’t skip)
        final OneTimeWorkRequest workRequest = QueryRefreshWorker.main(account.getId());
        final WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());
        workManager.enqueueUniqueWork(
                QueryRefreshWorker.uniqueName(account.getId()),
                ExistingWorkPolicy.KEEP,
                workRequest);
    }

    public void onNewToken(final String token) {}

    public static boolean register(
            final Context context, final List<AccountWithCredentials> accounts) {
        assertIsNotMainThread();
        if (GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context) == 0) {
            final PushManager pushManager = new PushManager(context);
            AppDatabase appDatabase = AppDatabase.getInstance(context);
            accounts.stream()
                    .map(AccountWithCredentials::getId)
                    .map(id -> appDatabase.accountDao().getCredentialsForAccount(id))
                    .distinct()
                    .forEach(pushManager::register);
            return true;
        } else {
            return false;
        }
    }

    private void register(final CredentialsEntity credentials) {
        final Task<String> task = FirebaseMessaging.getInstance().getToken();
        task.addOnSuccessListener(
                token -> {
                    register(credentials, token);
                });
    }

    private void register(final CredentialsEntity credentials, final String token) {
        final Task<String> task = FirebaseInstallations.getInstance().getId();
        task.addOnSuccessListener(
                installationId -> {
                    Futures.addCallback(
                            register(credentials, token, installationId),
                            new FutureCallback<>() {
                                @Override
                                public void onSuccess(@Nullable Boolean success) {
                                    LOGGER.info(
                                            "Push Subscription created. Success={}",
                                            Boolean.TRUE.equals(success));
                                }

                                @Override
                                public void onFailure(@NonNull final Throwable throwable) {
                                    LOGGER.info("Unable to create push subscription", throwable);
                                }
                            },
                            MoreExecutors.directExecutor());
                });
    }

    private ListenableFuture<Boolean> register(
            final CredentialsEntity credentials, final String token, final String installationId) {
        final String url =
                getPushUriTemplate().set("token", token).set("cid", credentials.id).expand();
        final PushSubscription pushSubscription =
                PushSubscription.builder().deviceClientId(installationId).url(url).build();
        LOGGER.info("attempting push subscription {}", pushSubscription);
        final JmapClient jmapClient = JmapClients.of(context.getApplicationContext(), credentials);
        final SetPushSubscriptionMethodCall setPushSubscription =
                SetPushSubscriptionMethodCall.builder()
                        .create(ImmutableMap.of("ps0", pushSubscription))
                        .build();
        final ListenableFuture<MethodResponses> methodResponsesFuture =
                jmapClient.call(setPushSubscription);
        return Futures.transform(
                methodResponsesFuture,
                methodResponses -> {
                    final SetPushSubscriptionMethodResponse response =
                            methodResponses.getMain(SetPushSubscriptionMethodResponse.class);
                    return response.getCreated().size() >= 1;
                },
                MoreExecutors.directExecutor());
    }

    private UriTemplate getPushUriTemplate() {
        return UriTemplate.buildFromTemplate(context.getString(R.string.push_url)).build();
    }

    private static void assertIsNotMainThread() {
        if (Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("should not be called from the main thread.");
        }
    }
}
