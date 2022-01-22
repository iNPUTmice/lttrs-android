package rs.ltt.android.ui;

import androidx.annotation.NonNull;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import rs.ltt.android.ui.preview.Size;

public class PreviewMeasurements {

    public final int sampleSize;
    public final int x;
    public final int y;
    public final int width;
    public final int height;

    @NonNull
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("sampleSize", sampleSize)
                .add("x", x)
                .add("y", y)
                .add("width", width)
                .add("height", height)
                .toString();
    }

    private PreviewMeasurements(
            final int sampleSize, final int x, final int y, final int width, final int height) {
        this.sampleSize = sampleSize;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public static PreviewMeasurements of(
            final int inWidth, final int inHeight, final Size outSize) {
        return of(inWidth, inHeight, outSize.width, outSize.height);
    }

    public static PreviewMeasurements of(
            final int inWidth, final int inHeight, final int outWidth, final int outHeight) {
        Preconditions.checkArgument(
                outWidth > outHeight,
                "This method is optimized to produce landscape preview. OutWidth must be greater"
                        + " than outHeight");
        final int sampleSize = calculateSampleSize(inWidth, inHeight, outWidth, outHeight);
        if (sampleSize > 1) {
            final PreviewMeasurements measurements =
                    of(inWidth / sampleSize, inHeight / sampleSize, outWidth, outHeight);
            if (measurements.sampleSize != 1) {
                throw new IllegalStateException("Sample size should have been 1 after second pass");
            }
            return new PreviewMeasurements(
                    sampleSize,
                    measurements.x,
                    measurements.y,
                    measurements.width,
                    measurements.height);
        }
        if (inWidth < outWidth && inHeight < outHeight) {
            return new PreviewMeasurements(sampleSize, 0, 0, inWidth, inHeight);
        }
        // landscape and square images get their preview cut from the middle
        final float targetAspectRatio = (float) outWidth / outHeight;
        final boolean isInLandscape = inWidth >= inHeight;
        final int optimalHeight = Math.round(inWidth / targetAspectRatio);
        final int optimalWidth = Math.round(inHeight * targetAspectRatio);
        if (isInLandscape) {
            final float inAspectRatio = (float) inWidth / inHeight;
            if (inAspectRatio > targetAspectRatio) {
                final int width = Math.max(optimalWidth, outWidth);
                final int widthDifference = inWidth - width;
                final int height = Math.min(optimalHeight, inHeight);
                return new PreviewMeasurements(sampleSize, widthDifference / 2, 0, width, height);
            } else {
                final int height = Math.max(optimalHeight, outHeight);
                final int heightDifference = inHeight - height;
                final int width = Math.min(optimalWidth, inWidth);
                return new PreviewMeasurements(sampleSize, 0, heightDifference / 2, width, height);
            }
        } else {
            final int height = Math.max(optimalHeight, outHeight);
            return new PreviewMeasurements(sampleSize, 0, 0, inWidth, height);
        }
    }

    private static int calculateSampleSize(
            final int inWidth, final int inHeight, final int outWidth, final int outHeight) {
        int sampleSize = 1;
        if (inHeight > outHeight || inWidth > outWidth) {
            final int halfHeight = inHeight / 2;
            final int halfWidth = inWidth / 2;
            while ((halfHeight / sampleSize) >= outHeight && (halfWidth / sampleSize) >= outWidth) {
                sampleSize *= 2;
            }
        }
        return sampleSize;
    }
}
