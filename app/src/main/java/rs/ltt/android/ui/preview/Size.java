package rs.ltt.android.ui.preview;

import androidx.annotation.NonNull;
import com.google.common.base.MoreObjects;

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
}
