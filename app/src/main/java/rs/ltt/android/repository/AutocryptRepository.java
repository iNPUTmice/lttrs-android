package rs.ltt.android.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import rs.ltt.android.MuaPool;
import rs.ltt.autocrypt.client.AbstractAutocryptClient;
import rs.ltt.autocrypt.client.header.EncryptionPreference;
import rs.ltt.autocrypt.jmap.AutocryptClient;
import rs.ltt.autocrypt.jmap.AutocryptPlugin;

public class AutocryptRepository extends AbstractRepository {

    public AutocryptRepository(final Application application, final long accountId) {
        super(application, accountId);
    }

    public ListenableFuture<Void> ensureEverythingIsSetup() {
        return Futures.transformAsync(
                getAutocryptClient(),
                AbstractAutocryptClient::ensureEverythingIsSetup,
                MoreExecutors.directExecutor());
    }

    public LiveData<Boolean> isAutocryptEnabled(final String userId) {
        return this.database.autocryptDao().isAutocryptEnabled(userId);
    }

    public LiveData<EncryptionPreference> getEncryptionPreference(final String userId) {
        return this.database.autocryptDao().getEncryptionPreference(userId);
    }

    public ListenableFuture<Void> setEncryptionPreference(
            final EncryptionPreference encryptionPreference) {
        return Futures.transformAsync(
                getAutocryptClient(),
                autocryptClient -> autocryptClient.setEncryptionPreference(encryptionPreference),
                MoreExecutors.directExecutor());
    }

    public ListenableFuture<Void> setEnabled(boolean enabled) {
        return Futures.transformAsync(
                getAutocryptClient(),
                autocryptClient -> autocryptClient.setEnabled(enabled),
                MoreExecutors.directExecutor());
    }

    private ListenableFuture<AutocryptClient> getAutocryptClient() {
        return Futures.transform(
                MuaPool.getInstance(application, accountId),
                mua -> mua.getPlugin(AutocryptPlugin.class).getAutocryptClient(),
                MoreExecutors.directExecutor());
    }
}
