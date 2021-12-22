package rs.ltt.android.ui;

import android.content.Context;
import android.util.TypedValue;
import androidx.annotation.AnyRes;
import androidx.annotation.AttrRes;

public class MaterialBackgrounds {

    public static @AnyRes int getBackgroundResource(
            final Context context, final @AttrRes int attributeResId) {
        final var typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(attributeResId, typedValue, true)) {
            return typedValue.resourceId;
        }
        throw new IllegalStateException("Unable to resolve background resource");
    }
}
