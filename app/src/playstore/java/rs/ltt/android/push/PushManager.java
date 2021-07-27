package rs.ltt.android.push;

import android.content.Context;
import android.os.Looper;

import com.damnhandy.uri.template.UriTemplate;
import com.google.android.gms.tasks.Task;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import rs.ltt.android.R;
import rs.ltt.android.database.AppDatabase;
import rs.ltt.android.entity.AccountWithCredentials;
import rs.ltt.android.entity.CredentialsEntity;
import rs.ltt.android.util.JmapClients;
import rs.ltt.jmap.client.JmapClient;
import rs.ltt.jmap.client.MethodResponses;
import rs.ltt.jmap.common.entity.PushMessage;
import rs.ltt.jmap.common.entity.PushSubscription;
import rs.ltt.jmap.common.method.call.core.SetPushSubscriptionMethodCall;
import rs.ltt.jmap.common.method.response.core.GetPushSubscriptionMethodResponse;

public class PushManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushManager.class);

    private final Context context;

    public PushManager(final Context context) {
        this.context = context;
    }

    public void onMessageReceived(final long cid, final PushMessage pushMessage) {

    }

    public void onNewToken(final String token) {

    }

    public static void register(final Context context, final List<AccountWithCredentials> accounts) {
        final PushManager pushManager = new PushManager(context);
        AppDatabase appDatabase = AppDatabase.getInstance(context);
        accounts.stream().map(AccountWithCredentials::getId)
                .map(id -> appDatabase.accountDao().getCredentialsForAccount(id))
                .distinct()
                .forEach(pushManager::register);
    }

    public void register(final CredentialsEntity credentials) {
        assertIsNotMainThread();
        final Task<String> task = FirebaseMessaging.getInstance().getToken();
        task.addOnSuccessListener(token -> {
            register(credentials, token);
        });
    }

    private void register(final CredentialsEntity credentials, final String token) {
        final Task<String> task = FirebaseInstallations.getInstance().getId();
        task.addOnSuccessListener(installationId -> {
            register(credentials, token, installationId);
        });
    }

    private void register(final CredentialsEntity credentials, final String token, final String installationId) {
        final String url = getPushUriTemplate()
                .set("token", token)
                .set("cid", credentials.id)
                .expand();
        final PushSubscription pushSubscription = PushSubscription.builder()
                .deviceClientId(installationId)
                .url(url)
                .build();
        LOGGER.info("attempting push subscription {}", pushSubscription);
        LOGGER.info("username {} password {} sr {}", credentials.username, credentials.password, credentials.sessionResource);
        final JmapClient jmapClient = JmapClients.of(context.getApplicationContext(), credentials);
        final SetPushSubscriptionMethodCall setPushSubscription = SetPushSubscriptionMethodCall.builder()
                .create(ImmutableMap.of("ps0", pushSubscription))
                .build();
        final ListenableFuture<MethodResponses> methodResponsesFuture = jmapClient.call(setPushSubscription);
        Futures.addCallback(methodResponsesFuture, new FutureCallback<MethodResponses>() {
            @Override
            public void onSuccess(@Nullable MethodResponses methodResponses) {
                final GetPushSubscriptionMethodResponse response = methodResponses.getMain(GetPushSubscriptionMethodResponse.class);
                LOGGER.info("Push Subscription created");
            }

            @Override
            public void onFailure(@NotNull final Throwable throwable) {
                LOGGER.info("Unable to create push subscription", throwable);
            }
        }, MoreExecutors.directExecutor());
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
