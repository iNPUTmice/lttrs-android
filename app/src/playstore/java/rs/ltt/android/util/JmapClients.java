package rs.ltt.android.util;

import android.content.Context;
import rs.ltt.android.entity.CredentialsEntity;
import rs.ltt.jmap.client.JmapClient;
import rs.ltt.jmap.client.session.FileSessionCache;

public final class JmapClients {

    private JmapClients() {}

    public static JmapClient of(final Context context, final CredentialsEntity credentials) {
        final JmapClient jmapClient =
                new JmapClient(
                        credentials.username, credentials.password, credentials.sessionResource);
        jmapClient.setSessionCache(new FileSessionCache(context.getCacheDir()));
        return jmapClient;
    }
}
