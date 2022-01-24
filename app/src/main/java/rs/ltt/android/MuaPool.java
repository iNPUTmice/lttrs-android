package rs.ltt.android;

import android.content.Context;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.cache.AutocryptDatabaseStorage;
import rs.ltt.android.cache.DatabaseCache;
import rs.ltt.android.database.AppDatabase;
import rs.ltt.android.database.LttrsDatabase;
import rs.ltt.android.entity.AccountWithCredentials;
import rs.ltt.autocrypt.client.storage.Storage;
import rs.ltt.autocrypt.jmap.AutocryptPlugin;
import rs.ltt.jmap.client.session.FileSessionCache;
import rs.ltt.jmap.mua.Mua;

public final class MuaPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(MuaPool.class);

    private static final Map<AccountWithCredentials, Mua> INSTANCES = new HashMap<>();

    private MuaPool() {}

    public static ListenableFuture<Mua> getInstance(final Context context, final long accountId) {
        return Futures.transform(
                AppDatabase.getInstance(context).accountDao().getAccountFuture(accountId),
                account -> getInstance(context, account),
                MoreExecutors.directExecutor());
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
            final Storage storage = new AutocryptDatabaseStorage(database);
            final AutocryptPlugin autocryptPlugin = new AutocryptPlugin(account.getName(), storage);
            final Mua mua =
                    Mua.builder()
                            .username(account.getUsername())
                            .password(account.getPassword())
                            .accountId(account.getAccountId())
                            .sessionResource(account.getSessionResource())
                            .useWebSocket(true)
                            .cache(new DatabaseCache(database))
                            .sessionCache(new FileSessionCache(application.getCacheDir()))
                            .plugin(AutocryptPlugin.class, autocryptPlugin)
                            .useWebSocket(false)
                            .queryPageSize(20L)
                            .build();
            INSTANCES.put(account, mua);
            return mua;
        }
    }

    public static void evict(long id) {
        synchronized (MuaPool.class) {
            final Iterator<Map.Entry<AccountWithCredentials, Mua>> iterator =
                    INSTANCES.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry<AccountWithCredentials, Mua> entry = iterator.next();
                final AccountWithCredentials account = entry.getKey();
                if (account.getId().equals(id)) {
                    final Mua mua = entry.getValue();
                    mua.close();
                    LOGGER.debug("Evicting {} from MuaPool", account.getAccountId());
                    iterator.remove();
                    return;
                }
            }
        }
    }
}
