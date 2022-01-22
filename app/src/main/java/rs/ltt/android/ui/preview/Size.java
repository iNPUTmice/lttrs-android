package rs.ltt.android.ui.preview;

import androidx.annotation.NonNull;
import com.google.common.base.MoreObjects;
import com.google.common.math.IntMath;
import java.math.RoundingMode;

public class Size {
    public final int width;
    public final int height;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @NonNull
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("width", width)
                .add("height", height)
                .toString();
    }

    public Size scaleTo(final Size size) {
        if (width > size.width && height > size.height) {
            final int horizontalScale = IntMath.divide(width, size.width, RoundingMode.DOWN);
            final int verticalScale = IntMath.divide(height, size.height, RoundingMode.DOWN);
            final int scale = Math.min(horizontalScale, verticalScale);
            return new Size(width / scale, height / scale);
        } else {
            final int horizontalScale = IntMath.divide(size.width, width, RoundingMode.UP);
            final int verticalScale = IntMath.divide(size.height, height, RoundingMode.UP);
            final int scale = scale(horizontalScale, verticalScale);
            return new Size(scale * width, scale * height);
        }
    }

    private static int scale(final int horizontalScale, final int verticalScale) {
        if (Math.abs(horizontalScale - verticalScale) > 3) {
            return Math.min(horizontalScale, verticalScale);
        } else {
            return Math.max(horizontalScale, verticalScale);
        }
    }
}
