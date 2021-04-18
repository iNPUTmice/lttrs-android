package rs.ltt.android;

import android.content.Context;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import rs.ltt.android.cache.DatabaseCache;
import rs.ltt.android.database.LttrsDatabase;
import rs.ltt.android.entity.AccountWithCredentials;
import rs.ltt.jmap.client.session.FileSessionCache;
import rs.ltt.jmap.mua.Mua;

public final class MuaPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(MuaPool.class);

    //TODO add expiry listener that calls close()
    private static final Cache<AccountWithCredentials, Mua> CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    private MuaPool() {

    }

    public static Mua getInstance(final Context context, final AccountWithCredentials account) throws ExecutionException {
        return CACHE.get(account, () -> {
            LOGGER.info("Building Mua for account id {}", account.getId());
            final Context application = context.getApplicationContext();
            final LttrsDatabase database = LttrsDatabase.getInstance(context, account.getId());
            return Mua.builder()
                    .username(account.username)
                    .password(account.password)
                    .accountId(account.accountId)
                    .sessionResource(account.sessionResource)
                    .useWebSocket(true)
                    .cache(new DatabaseCache(database))
                    .sessionCache(new FileSessionCache(application.getCacheDir()))
                    .queryPageSize(20L)
                    .build();
        });
    }

    public static Mua getInstanceUnchecked(final Context context, final AccountWithCredentials account) {
        try {
            return getInstance(context, account);
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause != null) {
                throw new RuntimeException(cause);
            } else {
                throw new RuntimeException();
            }
        }
    }

}
