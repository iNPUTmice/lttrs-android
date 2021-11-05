package rs.ltt.android.ui.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.DrawableRes;
import androidx.core.app.NotificationCompat;

import rs.ltt.android.R;
import rs.ltt.android.ui.activity.MainActivity;
import rs.ltt.jmap.client.event.State;

public class ForegroundServiceNotification extends AbstractNotification {

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
        notificationChannel.setSound(null, null);
        notificationChannel.setShowBadge(false);
        notificationManager.createNotificationChannel(notificationChannel);
    }

    public static Notification get(final Context context) {
        return get(context, State.CLOSED);
    }

    public static void updateConnectionState(final Context context, final State state) {
        final Notification notification = get(context, state);
        final NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.notify(ID, notification);
    }

    private static Notification get(final Context context, final State state) {
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                context,
                NOTIFICATION_CHANNEL_ID
        );
        notificationBuilder.setContentTitle(context.getString(R.string.foreground_service));
        notificationBuilder.setContentText(context.getString(R.string.foreground_service_notification_text));
        notificationBuilder.setSmallIcon(drawable(state));
        notificationBuilder.setShowWhen(false);
        notificationBuilder.setColor(getColor(context, R.attr.colorPrimary));
        notificationBuilder.setContentIntent(launchLttrs(context));
        return notificationBuilder.build();
    }

    private static @DrawableRes
    int drawable(final State state) {
        switch (state) {
            case FAILED:
                return R.drawable.ic_baseline_sync_problem_24;
            case CONNECTED:
                return R.drawable.ic_baseline_sync_24;
            case CLOSED:
            case CONNECTING:
                return R.drawable.ic_baseline_sync_disabled_24;
            default:
                throw new IllegalArgumentException(String.format("%s is an unknown state", state));
        }
    }

    private static PendingIntent launchLttrs(final Context context) {
        final Intent main = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(context, 0, main, PendingIntent.FLAG_IMMUTABLE);
    }
}
