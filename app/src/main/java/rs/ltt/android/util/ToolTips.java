package rs.ltt.android.util;

import android.widget.ImageButton;
import androidx.appcompat.widget.TooltipCompat;

public class ToolTips {

    private ToolTips() {}

    public static void apply(final ImageButton imageButton) {
        final CharSequence cd = imageButton.getContentDescription();
        if (CharSequences.isNullOrEmpty(cd)) {
            return;
        }
        TooltipCompat.setTooltipText(imageButton, cd);
    }
}
