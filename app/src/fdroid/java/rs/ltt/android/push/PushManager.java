package rs.ltt.android.push;

import android.app.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import rs.ltt.android.entity.AccountWithCredentials;

public final class PushManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushManager.class);

    private PushManager() {

    }

    public static void register(final Application application, final List<AccountWithCredentials> credentials) {
        LOGGER.info("Not registering for Firebase Messaging");
    }
}