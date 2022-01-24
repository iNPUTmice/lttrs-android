package rs.ltt.android.ui.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.R;
import rs.ltt.android.database.LttrsDatabase;
import rs.ltt.android.entity.AccountName;
import rs.ltt.android.entity.AccountWithCredentials;
import rs.ltt.android.entity.EmailWithBodiesAndSubject;
import rs.ltt.android.entity.From;
import rs.ltt.android.entity.Preview;
import rs.ltt.android.ui.AvatarDrawable;
import rs.ltt.android.ui.activity.LttrsActivity;
import rs.ltt.jmap.mua.util.KeywordUtil;

public class EmailNotification extends AbstractNotification {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotification.class);

    private static final int ID = 2;
    private static final int SUMMARY_ID = 3;

    private static final String NOTIFICATION_CHANNEL_ID = "email-channel-%d";
    private static final String NOTIFICATION_CHANNEL_GROUP = "group-%d";
    private static final String NOTIFICATION_TAG_SUMMARY = "summary-%d";
    private final NotificationManager notificationManager;
    private final Context context;
    private final AccountName account;
    private final List<EmailWithBodiesAndSubject> addedEmails;
    private final List<String> dismissedEmails;
    private final List<EmailWithBodiesAndSubject> allEmails;

    private EmailNotification(
            final Context context,
            final AccountName account,
            final List<EmailWithBodiesAndSubject> addedEmails,
            final List<String> dismissedEmails,
            final List<EmailWithBodiesAndSubject> allEmails) {
        this.context = context;
        this.notificationManager = context.getSystemService(NotificationManager.class);
        this.account = account;
        this.addedEmails = addedEmails;
        this.dismissedEmails = dismissedEmails;
        this.allEmails = allEmails;
    }

    public static void createChannel(final Context context, final AccountWithCredentials account) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        final NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);

        final NotificationChannelGroup notificationChannelGroup =
                new NotificationChannelGroup(notificationChannelGroup(account), account.getName());
        notificationManager.createNotificationChannelGroup(notificationChannelGroup);

        final NotificationChannel notificationChannel =
                new NotificationChannel(
                        notificationChannelId(account),
                        context.getString(R.string.notification_channel_name_email),
                        NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.setGroup(notificationChannelGroup(account));
        notificationChannel.setShowBadge(true);
        notificationManager.createNotificationChannel(notificationChannel);
    }

    public static void deleteChannel(final Context context, final Long accountId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        final NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);
        notificationManager.deleteNotificationChannel(notificationChannelId(accountId));
        notificationManager.deleteNotificationChannelGroup(notificationChannelGroup(accountId));
    }

    private static String notificationChannelGroup(final long accountId) {
        return String.format(Locale.US, NOTIFICATION_CHANNEL_GROUP, accountId);
    }

    private static String notificationChannelGroup(final AccountWithCredentials account) {
        return notificationChannelGroup(account.getId());
    }

    private static String notificationChannelId(final AccountWithCredentials account) {
        return notificationChannelId(account.getId());
    }

    private static String notificationChannelId(final Long accountId) {
        return String.format(Locale.US, NOTIFICATION_CHANNEL_ID, accountId);
    }

    private static String notificationTagSummary(final Long accountId) {
        return String.format(Locale.US, NOTIFICATION_TAG_SUMMARY, accountId);
    }

    private static List<String> combine(final List<String> a, final List<String> b) {
        return new ImmutableList.Builder<String>().addAll(a).addAll(b).build();
    }

    private static String getFromAsString(final Context context, final From from) {
        if (from instanceof From.Named) {
            return ((From.Named) from).getName();
        } else {
            return context.getString(R.string.draft);
        }
    }

    private static List<Tag> getActiveTags(final Context context) {
        final NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);
        final StatusBarNotification[] activeNotifications =
                notificationManager.getActiveNotifications();
        final ImmutableList.Builder<Tag> tagsBuilder = new ImmutableList.Builder<>();
        for (final StatusBarNotification notification : activeNotifications) {
            if (notification.getId() != ID) {
                continue;
            }
            try {
                tagsBuilder.add(Tag.parse(notification.getTag()));
            } catch (final Exception e) {
                // ignored
            }
        }
        return tagsBuilder.build();
    }

    private static List<String> getActiveEmailIds(final Context context, final Long accountId) {
        return getActiveTags(context).stream()
                .filter(tag -> tag.accountId.equals(accountId))
                .map(Tag::getEmailId)
                .collect(Collectors.toList());
    }

    private static String getGroupKey(final AccountName account) {
        return String.format(Locale.US, "emails-%d", account.id);
    }

    public static EmailNotification.Builder builder() {
        return new EmailNotification.Builder();
    }

    public static void cancel(final Context context, final long accountId) {
        final NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);
        getActiveTags(context).stream()
                .filter(tag -> tag.accountId.equals(accountId))
                .forEach(tag -> notificationManager.cancel(tag.toString(), ID));
        notificationManager.cancel(notificationTagSummary(accountId), SUMMARY_ID);
    }

    public static void cancel(final Context context, final Tag tag) {
        LOGGER.info("Dismissing {}", tag.emailId);
        final NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);
        notificationManager.cancel(tag.toString(), ID);
        if (getActiveTags(context).isEmpty()) {
            notificationManager.cancel(notificationTagSummary(tag.getAccountId()), SUMMARY_ID);
        }
    }

    public static Void cancel(
            final Context context, final long accountId, final List<String> emailIds) {
        final NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);
        final List<Tag> tags =
                emailIds.stream().map(id -> new Tag(accountId, id)).collect(Collectors.toList());
        final List<Tag> activeTags = getActiveTags(context);
        if (activeTags.isEmpty()) {
            return null;
        }
        int dismissed = 0;
        for (final Tag tag : tags) {
            if (activeTags.contains(tag)) {
                notificationManager.cancel(tag.toString(), ID);
                dismissed++;
            }
        }
        if (dismissed >= activeTags.size()) {
            LOGGER.info("Dismissed {} notifications. cancelled summary", dismissed);
            notificationManager.cancel(notificationTagSummary(accountId), SUMMARY_ID);
        } else if (dismissed > 0) {
            LOGGER.info("Dismissed {} notifications", dismissed);
        }
        return null;
    }

    public void refresh() {
        LOGGER.info(
                "added {}, dismissed {}, total {}",
                addedEmails.size(),
                dismissedEmails.size(),
                allEmails.size());
        for (final String id : dismissedEmails) {
            dismiss(id);
        }
        if (allEmails.isEmpty()) {
            notificationManager.cancel(notificationTagSummary(account.getId()), SUMMARY_ID);
            return;
        }
        for (final EmailWithBodiesAndSubject email : addedEmails) {
            final Tag tag = new Tag(account.id, email.getId());
            final Notification notification = get(email);
            notificationManager.notify(tag.toString(), ID, notification);
        }
        if (addedEmails.size() > 0 || dismissedEmails.size() > 0) {
            final Notification summaryNotification = getSummary(allEmails);
            notificationManager.notify(
                    notificationTagSummary(account.getId()), SUMMARY_ID, summaryNotification);
        }
    }

    private Notification get(final EmailWithBodiesAndSubject email) {
        final From from = email.getFirstFrom();
        final AvatarDrawable avatar = new AvatarDrawable(context, from);
        final String subject = getSubject(email);
        final Preview preview = email.getPreview();
        final String bigText;
        if (Strings.isNullOrEmpty(preview.getPreview())) {
            bigText = subject;
        } else {
            bigText = String.format("%s\n%s", subject, preview.getPreview());
        }
        final NotificationCompat.BigTextStyle bigTextStyle =
                new NotificationCompat.BigTextStyle().bigText(bigText);
        return new NotificationCompat.Builder(context, notificationChannelId(account.getId()))
                .setSmallIcon(R.drawable.ic_email_outline_24dp)
                .setContentTitle(getFromAsString(context, from))
                .setContentText(subject)
                .setSubText(account.getName())
                .setLargeIcon(avatar.toBitmap())
                .setWhen(email.getEffectiveDate().toEpochMilli())
                .setStyle(bigTextStyle)
                .setColor(getColor(context, R.attr.colorPrimary))
                .setGroup(getGroupKey(account))
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                .setContentIntent(getPendingIntent(email))
                .build();
    }

    private PendingIntent getPendingIntent(final EmailWithBodiesAndSubject email) {
        final Tag tag = new Tag(account.getId(), email.getId());
        final Intent intent = LttrsActivity.viewIntent(context, tag, email.threadId);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification getSummary(final List<EmailWithBodiesAndSubject> emails) {
        final NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        for (final EmailWithBodiesAndSubject email : emails) {
            inboxStyle.addLine(
                    String.format(
                            "<b>%s</b> %s",
                            getFromAsString(context, email.getFirstFrom()), getSubject(email)));
        }
        return new NotificationCompat.Builder(context, notificationChannelId(account.getId()))
                .setSmallIcon(R.drawable.ic_email_outline_24dp)
                .setSubText(account.getName())
                .setContentTitle(
                        context.getResources()
                                .getQuantityString(
                                        R.plurals.x_new_emails, emails.size(), emails.size()))
                .setStyle(inboxStyle)
                .setColor(getColor(context, R.attr.colorPrimary))
                .setGroup(getGroupKey(account))
                .setGroupSummary(true)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                .build();
    }

    private String getSubject(final EmailWithBodiesAndSubject email) {
        return Strings.isNullOrEmpty(email.subject)
                ? context.getString(R.string.no_subject)
                : email.subject;
    }

    private void dismiss(final String id) {
        final Tag tag = new Tag(account.getId(), id);
        final NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);
        notificationManager.cancel(tag.toString(), ID);
    }

    public static class Builder {
        private Context context;
        private AccountName account;
        private List<String> freshlyAddedEmailIds = Collections.emptyList();

        public Builder setAccount(final AccountName account) {
            this.account = account;
            return this;
        }

        public Builder setContext(final Context context) {
            this.context = context;
            return this;
        }

        public Builder setFreshlyAddedEmailIds(List<String> freshlyAddedEmailIds) {
            this.freshlyAddedEmailIds = freshlyAddedEmailIds;
            return this;
        }

        public EmailNotification build() {
            Preconditions.checkNotNull(context, "Supplied context must not be null");
            Preconditions.checkNotNull(account, "Supplied account must not be null");
            final List<String> activeEmailNotifications =
                    getActiveEmailIds(context, account.getId());
            final LttrsDatabase database = LttrsDatabase.getInstance(context, account.getId());
            final List<EmailWithBodiesAndSubject> emails =
                    database.threadAndEmailDao()
                            .getEmails(combine(freshlyAddedEmailIds, activeEmailNotifications));

            final ImmutableList.Builder<EmailWithBodiesAndSubject> allNotificationBuilder =
                    ImmutableList.builder();
            final ImmutableList.Builder<EmailWithBodiesAndSubject> addedNotificationBuilder =
                    ImmutableList.builder();
            final ImmutableList.Builder<String> dismissedNotificationBuilder =
                    ImmutableList.builder();
            for (final EmailWithBodiesAndSubject email : emails) {
                // TODO Take keyword overwrite into account
                if (KeywordUtil.seen(email)) {
                    if (activeEmailNotifications.contains(email.getId())) {
                        dismissedNotificationBuilder.add(email.getId());
                    }
                } else {
                    allNotificationBuilder.add(email);
                    if (!activeEmailNotifications.contains(email.getId())) {
                        addedNotificationBuilder.add(email);
                    }
                }
            }
            return new EmailNotification(
                    context,
                    account,
                    addedNotificationBuilder.build(),
                    dismissedNotificationBuilder.build(),
                    allNotificationBuilder.build());
        }
    }

    public static class Tag {

        private static final String SCHEME = "lttrs";

        private final Long accountId;
        private final String emailId;

        public Tag(final Long accountId, final String emailId) {
            Preconditions.checkNotNull(accountId);
            Preconditions.checkNotNull(emailId);
            this.accountId = accountId;
            this.emailId = emailId;
        }

        public static Tag parse(final Uri uri) {
            if (SCHEME.equals(uri.getScheme())) {
                final String path = uri.getPath();
                if (path.length() < 1) {
                    throw new IllegalArgumentException("Path not long enough to contain ID");
                }
                return parse(uri.getAuthority(), path.substring(1));
            }
            throw new IllegalArgumentException("Unknown scheme");
        }

        public static Tag parse(final String tag) {
            final int separatorIndex = tag.indexOf('-');
            if (separatorIndex < 0 || separatorIndex + 1 >= tag.length()) {
                throw new IllegalArgumentException("Not a valid tag");
            }
            final String account = tag.substring(0, separatorIndex);
            final String emailId = tag.substring(separatorIndex + 1);
            return parse(account, emailId);
        }

        private static Tag parse(final String account, final String emailId) {
            final long accountId;
            try {
                accountId = Long.parseLong(account);
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("Not a valid account id");
            }
            return new Tag(accountId, emailId);
        }

        @NonNull
        @Override
        public String toString() {
            return String.format(Locale.US, "%d-%s", accountId, emailId);
        }

        public long getAccountId() {
            return accountId;
        }

        public String getEmailId() {
            return emailId;
        }

        public Uri toUri() {
            return new Uri.Builder()
                    .scheme(SCHEME)
                    .authority(String.valueOf(accountId))
                    .path(emailId)
                    .build();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tag tag = (Tag) o;
            return Objects.equal(accountId, tag.accountId) && Objects.equal(emailId, tag.emailId);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(accountId, emailId);
        }
    }
}
