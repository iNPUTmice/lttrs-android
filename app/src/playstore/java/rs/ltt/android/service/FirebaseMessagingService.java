package rs.ltt.android.service;

import androidx.annotation.NonNull;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.push.PushManager;
import rs.ltt.android.util.PushMessages;
import rs.ltt.jmap.common.entity.PushMessage;

public class FirebaseMessagingService
        extends com.google.firebase.messaging.FirebaseMessagingService {

    private final PushManager pushManager = new PushManager(this);

    private static final Logger LOGGER = LoggerFactory.getLogger(FirebaseMessagingService.class);

    @Override
    public void onNewToken(@NonNull final String token) {
        this.pushManager.onNewToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull final RemoteMessage remoteMessage) {
        LOGGER.info("onMessageReceived");
        final Map<String, String> data = remoteMessage.getData();
        final long cid;
        final PushMessage pushMessage;
        try {
            cid = PushMessages.getCredentialsId(data);
            pushMessage = PushMessages.of(data);
        } catch (final Exception e) {
            LOGGER.warn("Invalid push message received", e);
            return;
        }
        this.pushManager.onMessageReceived(cid, pushMessage);
    }
}
