package rs.ltt.android.ui.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import rs.ltt.android.R;
import rs.ltt.android.ui.activity.MainActivity;

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
        notificationBuilder.setShowWhen(false);
        notificationBuilder.setColor(context.getColor(R.color.colorPrimary));
        notificationBuilder.setContentIntent(launchLttrs(context));
        return notificationBuilder.build();
    }

    private static PendingIntent launchLttrs(final Context context) {
        final Intent main = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(context, 0, main, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
