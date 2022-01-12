package rs.ltt.android.ui;

import android.app.Activity;
import androidx.annotation.PluralsRes;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import rs.ltt.android.R;
import rs.ltt.android.util.Event;

public final class MaterialAlertDialogs {
    private MaterialAlertDialogs() {}

    public static void error(final Activity activity, final Event<String> event) {
        if (event.isConsumable()) {
            error(activity, event.consume());
        }
    }

    public static void error(final Activity activity, final String message) {
        new MaterialAlertDialogBuilder(activity)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    public static void error(
            final Activity activity,
            final @PluralsRes int pluralRes,
            final int quantity,
            final Object... objects) {
        error(activity, activity.getResources().getQuantityString(pluralRes, quantity, objects));
    }
}
