package rs.ltt.android;

import android.content.Context;

import com.google.common.collect.MapMaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import rs.ltt.android.cache.DatabaseCache;
import rs.ltt.android.database.LttrsDatabase;
import rs.ltt.android.entity.AccountWithCredentials;
import rs.ltt.jmap.client.session.FileSessionCache;
import rs.ltt.jmap.mua.Mua;

public final class MuaPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(MuaPool.class);

    private static final Map<AccountWithCredentials, Mua> INSTANCES = new HashMap<>();

    private MuaPool() {

    }

    public static Mua getInstance(final Context context, final AccountWithCredentials account) {
        final Mua instance = INSTANCES.get(account);
        if (instance != null) {
            return instance;
        }
        synchronized (MuaPool.class) {
            final Mua existing = INSTANCES.get(account);
            if (existing != null) {
                return existing;
            }
            LOGGER.info("Building Mua for account id {}", account.getId());
            final Context application = context.getApplicationContext();
            final LttrsDatabase database = LttrsDatabase.getInstance(context, account.getId());
            final Mua mua = Mua.builder()
                    .username(account.username)
                    .password(account.password)
                    .accountId(account.accountId)
                    .sessionResource(account.sessionResource)
                    .useWebSocket(true)
                    .cache(new DatabaseCache(database))
                    .sessionCache(new FileSessionCache(application.getCacheDir()))
                    .queryPageSize(20L)
                    .build();
            INSTANCES.put(account, mua);
            return mua;
        }
    }

}
