package rs.ltt.android.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mua.util.StandardQueries;

public class SearchQueryRefreshWorker extends QueryRefreshWorker {

    private static final String SEARCH_TERM_KEY = "searchTerm";

    private final String searchTerm;

    public SearchQueryRefreshWorker(
            @NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = workerParams.getInputData();
        this.searchTerm = data.getString(SEARCH_TERM_KEY);
    }

    public static Data data(
            final Long account, final boolean skipOverEmpty, final String searchTerm) {
        return new Data.Builder()
                .putLong(ACCOUNT_KEY, account)
                .putBoolean(SKIP_OVER_EMPTY_KEY, skipOverEmpty)
                .putString(SEARCH_TERM_KEY, searchTerm)
                .build();
    }

    @Override
    EmailQuery getEmailQuery() {
        return StandardQueries.search(
                searchTerm, getDatabase().mailboxDao().getMailboxes(Role.TRASH, Role.JUNK));
    }
}
