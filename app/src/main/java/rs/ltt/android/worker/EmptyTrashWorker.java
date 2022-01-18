package rs.ltt.android.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.entity.MailboxWithRoleAndName;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.mua.Mua;
import rs.ltt.jmap.mua.util.StandardQueries;

public class EmptyTrashWorker extends AbstractMuaWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmptyTrashWorker.class);

    EmptyTrashWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static Data data(long accountId) {
        return new Data.Builder().putLong(ACCOUNT_KEY, accountId).build();
    }

    @NonNull
    @Override
    public Result doWork() {
        final Mua mua = getMua();
        try {
            mua.emptyTrash().get();
        } catch (final ExecutionException e) {
            LOGGER.warn("Unable to empty trash", e);
            if (shouldRetry(e)) {
                return Result.retry();
            } else {
                return Result.failure(Failure.of(e.getCause()));
            }
        } catch (final InterruptedException e) {
            return Result.retry();
        }
        try {
            final MailboxWithRoleAndName trashMailbox =
                    getDatabase().mailboxDao().getMailbox(Role.TRASH);
            mua.query(StandardQueries.mailbox(trashMailbox)).get();
        } catch (final Exception e) {
            LOGGER.debug("Ignoring inability to refresh query", e);
        }
        return Result.success();
    }
}
