package rs.ltt.android.ui.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import rs.ltt.android.R;

public class ForegroundServiceNotification {

    public static final int ID = 1;

    private static final String NOTIFICATION_CHANNEL_ID = "foreground";

    public static void createChannel(final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        final NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        final NotificationChannel notificationChannel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.foreground_service),
                NotificationManager.IMPORTANCE_MIN
        );
        notificationManager.createNotificationChannel(notificationChannel);

    }

    public static Notification get(final Context context) {
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                context,
                NOTIFICATION_CHANNEL_ID
        );
        notificationBuilder.setContentTitle(context.getString(R.string.foreground_service));
        notificationBuilder.setContentText(context.getString(R.string.foreground_service_notification_text));
        notificationBuilder.setSmallIcon(R.drawable.ic_baseline_sync_24);
        return notificationBuilder.build();
    }
}
