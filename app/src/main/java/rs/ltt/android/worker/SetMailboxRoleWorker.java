package rs.ltt.android.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import com.google.common.base.Preconditions;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRole;
import rs.ltt.jmap.common.entity.Role;

public class SetMailboxRoleWorker extends AbstractMuaWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetMailboxRoleWorker.class);

    private static final String MAILBOX_ID_KEY = "mailboxId";
    private static final String MAILBOX_ROLE_KEY = "role";

    private final String mailboxId;
    private final Role role;

    public SetMailboxRoleWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = getInputData();
        this.mailboxId = Preconditions.checkNotNull(data.getString(MAILBOX_ID_KEY));
        this.role = Role.valueOf(data.getString(MAILBOX_ROLE_KEY));
    }

    public static Data data(Long account, String mailboxId, Role role) {
        return new Data.Builder()
                .putLong(ACCOUNT_KEY, account)
                .putString(MAILBOX_ID_KEY, mailboxId)
                .putString(MAILBOX_ROLE_KEY, role.toString())
                .build();
    }

    @NonNull
    @Override
    public Result doWork() {
        final IdentifiableMailboxWithRole mailbox =
                getDatabase().mailboxDao().getMailbox(this.mailboxId);
        try {
            getMua().setRole(mailbox, role).get();
            return Result.success();
        } catch (ExecutionException e) {
            LOGGER.warn("Unable to reassign role to mailbox", e);
            if (shouldRetry(e)) {
                return Result.retry();
            } else {
                return Result.failure(Failure.of(e.getCause()));
            }
        } catch (InterruptedException e) {
            return Result.retry();
        }
    }
}
