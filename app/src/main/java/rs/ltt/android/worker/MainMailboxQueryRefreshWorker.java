package rs.ltt.android.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import rs.ltt.android.database.AppDatabase;
import rs.ltt.android.database.LttrsDatabase;
import rs.ltt.android.entity.AccountName;
import rs.ltt.android.ui.notification.EmailNotification;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRole;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mua.util.StandardQueries;

public class MainMailboxQueryRefreshWorker extends QueryRefreshWorker {

    public MainMailboxQueryRefreshWorker(
            @NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private static List<String> freshlyAddedEmailIds(
            final Set<String> preexistingEmailIds, final List<String> postRefreshEmailIds) {
        final ImmutableList.Builder<String> freshlyAddedEmailIds = new ImmutableList.Builder<>();
        for (final String emailId : postRefreshEmailIds) {
            if (preexistingEmailIds.contains(emailId)) {
                return freshlyAddedEmailIds.build();
            }
            freshlyAddedEmailIds.add(emailId);
        }
        return freshlyAddedEmailIds.build();
    }

    public static Data data(final Long account, final boolean skipOverEmpty) {
        return new Data.Builder()
                .putLong(ACCOUNT_KEY, account)
                .putBoolean(SKIP_OVER_EMPTY_KEY, skipOverEmpty)
                .build();
    }

    public static String uniquePeriodicName(final Long accountId) {
        return String.format(Locale.ENGLISH, "account-%d-periodic-refresh", accountId);
    }

    @NonNull
    @Override
    protected Result refresh(final EmailQuery emailQuery)
            throws ExecutionException, InterruptedException {
        throwOnEmpty(emailQuery);
        final LttrsDatabase database = getDatabase();
        final Set<String> preexistingEmailIds =
                ImmutableSet.copyOf(database.queryDao().getEmailIds(emailQuery.asHash()));
        getMua().query(emailQuery).get();
        final List<String> freshlyAddedEmailIds =
                freshlyAddedEmailIds(
                        preexistingEmailIds, database.queryDao().getEmailIds(emailQuery.asHash()));
        final AccountName account =
                AppDatabase.getInstance(getApplicationContext())
                        .accountDao()
                        .getAccountName(this.account);
        // We deliberately decide to show notifications even for email that arrive while the app is
        // in foreground. Just because the user saw the email coming in doesn't mean they don’t want
        // to add it to their 'notification queue' and deal with it later.
        // However we may or may not want to suppress the sound when the app is in foreground.
        // If we decide to do so checking with `ProcessLifecycleOwner.get().getLifecycle()
        // .getCurrentState().isAtLeast(Lifecycle.State.STARTED);` in EmailNotification.Builder is
        // a good way for telling.
        EmailNotification.builder()
                .setAccount(account)
                .setContext(getApplicationContext())
                .setFreshlyAddedEmailIds(freshlyAddedEmailIds)
                .build()
                .refresh();
        return Result.success();
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
