package rs.ltt.android.repository;

import android.app.Application;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import rs.ltt.android.MuaPool;
import rs.ltt.jmap.mua.Mua;

public class AbstractMuaRepository extends AbstractRepository {

    protected final ListenableFuture<Mua> mua;

    AbstractMuaRepository(final Application application, final long accountId) {
        super(application, accountId);
        this.mua =
                Futures.transform(
                        getAccount(),
                        account -> MuaPool.getInstance(application, account),
                        MoreExecutors.directExecutor());
    }
}
