package rs.ltt.android.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import com.google.common.base.Preconditions;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mua.util.StandardQueries;

public class MailboxQueryRefreshWorker extends QueryRefreshWorker {

    private final String mailboxId;

    public MailboxQueryRefreshWorker(
            @NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = workerParams.getInputData();
        this.mailboxId =
                Preconditions.checkNotNull(data.getString(MAILBOX_ID_KEY), "mailboxId is required");
    }

    public static Data data(
            final Long account, final boolean skipOverEmpty, final String mailboxId) {
        return new Data.Builder()
                .putLong(ACCOUNT_KEY, account)
                .putBoolean(SKIP_OVER_EMPTY_KEY, skipOverEmpty)
                .putString(MAILBOX_ID_KEY, mailboxId)
                .build();
    }

    @Override
    EmailQuery getEmailQuery() {
        return StandardQueries.mailbox(mailboxId);
    }
}
