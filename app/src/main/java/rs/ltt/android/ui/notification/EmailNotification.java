package rs.ltt.android.ui.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

import rs.ltt.android.R;
import rs.ltt.android.entity.AccountName;
import rs.ltt.android.entity.EmailNotificationPreview;
import rs.ltt.android.entity.From;
import rs.ltt.android.ui.AvatarDrawable;

public class EmailNotification {

    public static final int ID = 2;
    private static final int SUMMARY_ID = 3;

    private static final String NOTIFICATION_CHANNEL_ID = "email";

    //TODO create notification channel for each account
    public static void createChannel(final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        final NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        final NotificationChannel notificationChannel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.foreground_service),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        notificationChannel.setShowBadge(true);
        notificationManager.createNotificationChannel(notificationChannel);

    }


    public static void notify(final Context context,
                              final AccountName account,
                              final List<EmailNotificationPreview> emails) {
        final NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (emails.isEmpty()) {
            notificationManager.cancel(SUMMARY_ID);
            return;
        }
        for (EmailNotificationPreview email : emails) {
            final Tag tag = new Tag(account.id, email.getId());
            final Notification notification = get(context, account, email);
            notificationManager.notify(tag.toString(), ID, notification);
        }
        final Notification summaryNotification = getSummary(context, account, emails);
        notificationManager.notify(SUMMARY_ID, summaryNotification);
    }


    private static Notification get(final Context context,
                                    final AccountName account,
                                    final EmailNotificationPreview email) {
        final From from = email.getFrom();
        final AvatarDrawable avatar = new AvatarDrawable(context, from);
        final NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle()
                .bigText(String.format("%s\n%s", email.subject, email.getText()));
        return new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_email_outline_24dp)
                .setContentTitle(getFromAsString(context, from))
                .setContentText(email.subject)
                .setSubText(account.getName())
                .setLargeIcon(avatar.toBitmap())
                .setWhen(email.receivedAt.toEpochMilli())
                .setStyle(bigTextStyle)
                .setColor(context.getColor(R.color.colorPrimary))
                .setGroup(getGroupKey(account))
                .build();
    }

    private static Notification getSummary(final Context context,
                                           final AccountName account,
                                           final List<EmailNotificationPreview> emails) {
        final NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        for (final EmailNotificationPreview email : emails) {
            inboxStyle.addLine(String.format(
                    "<b>%s</b> %s",
                    getFromAsString(context, email.getFrom()),
                    email.subject
            ));
        }
        return new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_email_outline_24dp)
                .setSubText(account.getName())
                .setContentTitle(
                        context.getResources().getQuantityString(R.plurals.x_new_emails, emails.size(), emails.size())
                )
                .setStyle(inboxStyle)
                .setColor(context.getColor(R.color.colorPrimary))
                .setGroup(getGroupKey(account))
                .setGroupSummary(true)
                .build();
    }

    private static String getFromAsString(final Context context, final From from) {
        if (from instanceof From.Named) {
            return ((From.Named) from).getName();
        } else {
            return context.getString(R.string.draft);
        }
    }

    public static List<String> getActiveEmailIds(final Context context, final Long accountId) {
        final NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        final StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
        final ImmutableList.Builder<String> emailIdsBuilder = new ImmutableList.Builder<>();
        for (final StatusBarNotification notification : activeNotifications) {
            if (notification.getId() != ID) {
                continue;
            }
            try {
                final Tag tag = Tag.parse(notification.getTag());
                emailIdsBuilder.add(tag.getEmailId());
            } catch (final Exception e) {
                //ignore
            }
        }
        return emailIdsBuilder.build();
    }

    private static String getGroupKey(final AccountName account) {
        return String.format(Locale.US, "emails-%d", account.id);
    }

    public static void dismiss(final Context context, final Long account, final String id) {
        final Tag tag = new Tag(account, id);
        final NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.cancel(tag.toString(), ID);
    }

    public static class Tag {

        private final Long accountId;
        private final String emailId;

        public Tag(final Long accountId, final String emailId) {
            Preconditions.checkNotNull(accountId);
            Preconditions.checkNotNull(emailId);
            this.accountId = accountId;
            this.emailId = emailId;
        }

        public static Tag parse(final String tag) {
            final int separatorIndex = tag.indexOf('-');
            if (separatorIndex < 0 || separatorIndex + 1 >= tag.length()) {
                throw new IllegalArgumentException("Not a valid tag");
            }
            final long accountId;
            try {
                accountId = Long.parseLong(tag.substring(0, separatorIndex));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Not a valid account id");
            }
            final String emailId = tag.substring(separatorIndex + 1);
            return new Tag(accountId, emailId);
        }

        @NotNull
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
    }

}
