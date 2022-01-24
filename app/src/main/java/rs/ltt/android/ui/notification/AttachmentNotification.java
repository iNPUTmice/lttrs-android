package rs.ltt.android.ui.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.work.WorkManager;
import java.util.Locale;
import java.util.UUID;
import rs.ltt.android.R;
import rs.ltt.jmap.common.entity.Downloadable;

public class AttachmentNotification {

    public static final int DOWNLOAD_ID = 4;
    public static final int UPLOAD_ID = 5;

    private static final String NOTIFICATION_CHANNEL_ID = "attachment";

    public static void createChannel(final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        final NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);

        final NotificationChannel notificationChannel =
                new NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        context.getString(R.string.attachments),
                        NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.setSound(null, null);
        notificationChannel.setShowBadge(false);
        notificationChannel.enableVibration(false);
        notificationChannel.enableLights(false);
        notificationManager.createNotificationChannel(notificationChannel);
    }

    public static Notification downloading(
            final Context context,
            final UUID id,
            final Downloadable downloadable,
            final int progress,
            final boolean indeterminate) {
        final NotificationCompat.Builder notificationBuilder =
                inProgressBuilder(context, id, downloadable.getName(), progress, indeterminate);
        notificationBuilder.setSmallIcon(R.drawable.ic_baseline_download_24);
        return notificationBuilder.build();
    }

    public static Notification uploading(
            final Context context,
            final UUID id,
            final String name,
            final int progress,
            final boolean indeterminate) {
        final NotificationCompat.Builder notificationBuilder =
                inProgressBuilder(context, id, name, progress, indeterminate);
        notificationBuilder.setSmallIcon(R.drawable.ic_baseline_file_upload_24);
        return notificationBuilder.build();
    }

    private static NotificationCompat.Builder inProgressBuilder(
            final Context context,
            final UUID id,
            final String name,
            final int progress,
            final boolean indeterminate) {
        final NotificationCompat.Action cancelAction =
                new NotificationCompat.Action(
                        R.drawable.ic_baseline_cancel_24,
                        context.getString(R.string.cancel),
                        WorkManager.getInstance(context).createCancelPendingIntent(id));
        final NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setContentTitle(name);
        if (indeterminate) {
            notificationBuilder.setProgress(1, 1, true);
        } else {
            notificationBuilder.setProgress(100, progress, false);
            notificationBuilder.setSubText(String.format(Locale.US, "%d%%", progress));
        }
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        notificationBuilder.setOngoing(true);
        notificationBuilder.setShowWhen(false);
        notificationBuilder.addAction(cancelAction);
        return notificationBuilder;
    }

    public static Notification downloaded(final Context context, final Downloadable downloadable) {
        final NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setContentTitle(context.getString(R.string.download_complete));
        notificationBuilder.setContentText(downloadable.getName());
        notificationBuilder.setSmallIcon(R.drawable.ic_baseline_download_done_24);
        notificationBuilder.setShowWhen(false);
        return notificationBuilder.build();
    }

    public static Notification emailNotCached(final Context context) {
        final NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setContentTitle(context.getString(R.string.attachment_download_failed));
        notificationBuilder.setContentText(context.getString(R.string.email_not_cached));
        notificationBuilder.setSmallIcon(R.drawable.ic_baseline_error_24);
        notificationBuilder.setShowWhen(false);
        return notificationBuilder.build();
    }

    public static Notification uploaded(final Context context, final String name) {
        final NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setContentTitle(context.getString(R.string.upload_complete));
        notificationBuilder.setContentText(name);
        notificationBuilder.setSmallIcon(R.drawable.ic_baseline_done_24);
        notificationBuilder.setShowWhen(false);
        return notificationBuilder.build();
    }
}
