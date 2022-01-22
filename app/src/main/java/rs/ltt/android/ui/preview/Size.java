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
        final int horizontalScale = IntMath.divide(size.width, width, RoundingMode.UP);
        final int verticalScale = IntMath.divide(size.height, height, RoundingMode.UP);
        final int scale = Math.max(horizontalScale, verticalScale);
        return new Size(scale * width, scale * height);
    }
}
