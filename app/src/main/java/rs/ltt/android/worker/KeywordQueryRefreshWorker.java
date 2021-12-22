package rs.ltt.android.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mua.util.StandardQueries;

public class KeywordQueryRefreshWorker extends QueryRefreshWorker {

    private final String keyword;

    public KeywordQueryRefreshWorker(
            @NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = workerParams.getInputData();
        this.keyword = data.getString(KEYWORD_KEY);
    }

    public static Data data(final Long account, final boolean skipOverEmpty, final String keyword) {
        return new Data.Builder()
                .putLong(ACCOUNT_KEY, account)
                .putBoolean(SKIP_OVER_EMPTY_KEY, skipOverEmpty)
                .putString(KEYWORD_KEY, keyword)
                .build();
    }

    @Override
    EmailQuery getEmailQuery() {
        return StandardQueries.keyword(
                keyword, getDatabase().mailboxDao().getMailboxes(Role.TRASH, Role.JUNK));
    }
}
