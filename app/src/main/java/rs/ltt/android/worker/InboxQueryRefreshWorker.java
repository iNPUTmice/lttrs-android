package rs.ltt.android.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRole;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mua.util.StandardQueries;

public class InboxQueryRefreshWorker extends QueryRefreshWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(InboxQueryRefreshWorker.class);

    public InboxQueryRefreshWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        final EmailQuery emailQuery = getEmailQuery();
        if (skipOverEmpty && getDatabase().queryDao().empty(emailQuery.asHash())) {
            LOGGER.warn("Do not refresh because query is empty (UI will automatically load this)");
            return Result.failure();
        }
        try {
            LOGGER.info("Refreshing {}", emailQuery);
            getMua().query(emailQuery).get();
            return Result.success();
        } catch (final Exception e) {
            LOGGER.info("Unable to refresh query", e);
            return Result.failure();
        }
    }

    public static Data data(final Long account, final boolean skipOverEmpty) {
        return new Data.Builder()
                .putLong(ACCOUNT_KEY, account)
                .putBoolean(SKIP_OVER_EMPTY_KEY, skipOverEmpty)
                .build();
    }

    @Override
    protected EmailQuery getEmailQuery() {
        final IdentifiableMailboxWithRole inbox = getDatabase().mailboxDao().getMailbox(Role.INBOX);
        if (inbox == null) {
            return EmailQuery.unfiltered();
        } else {
            return StandardQueries.mailbox(inbox);
        }
    }

}
