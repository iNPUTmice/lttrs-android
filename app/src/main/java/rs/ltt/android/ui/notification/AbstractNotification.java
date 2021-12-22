package rs.ltt.android.ui.notification;

import android.content.Context;
import android.util.TypedValue;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import rs.ltt.android.R;

public abstract class AbstractNotification {

    protected static @ColorInt int getColor(Context context, @AttrRes int colorAttributeResId) {
        final var typedValue = new TypedValue();
        final var theme = context.getResources().newTheme();
        theme.applyStyle(R.style.MainTheme, true);
        if (theme.resolveAttribute(colorAttributeResId, typedValue, true)) {
            return typedValue.data;
        }
        throw new IllegalStateException("Unable to resolve color attribute");
    }
}
