package rs.ltt.android.push;

import android.app.Application;
import java.util.List;
import rs.ltt.android.entity.AccountWithCredentials;

public final class PushManager {

    private PushManager() {}

    public static boolean register(
            final Application application, final List<AccountWithCredentials> credentials) {
        return false;
    }
}
