package rs.ltt.android.ui.preview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;
import com.google.common.net.MediaType;
import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.ui.PreviewMeasurements;

public class ImagePreview {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImagePreview.class);

    protected ImagePreview() {}

    public static Bitmap getImagePreview(
            final File file, final MediaType mediaType, final Size previewSize) {
        final BitmapFactory.Options calculationOptions = new BitmapFactory.Options();
        calculationOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), calculationOptions);
        if (calculationOptions.outWidth < 1 || calculationOptions.outHeight < 1) {
            throw new IllegalArgumentException(String.format("Could not decode %s", mediaType));
        }
        final int rotation = getRotation(file);
        final boolean isRotated = rotation == 90 || rotation == 270;
        final int imageWidth =
                isRotated ? calculationOptions.outHeight : calculationOptions.outWidth;
        final int imageHeight =
                isRotated ? calculationOptions.outWidth : calculationOptions.outHeight;
        final PreviewMeasurements previewMeasurements =
                PreviewMeasurements.of(
                        imageWidth, imageHeight, previewSize.width, previewSize.height);
        LOGGER.debug("original image size " + imageWidth + "x" + imageHeight);
        LOGGER.debug("preview size: " + previewSize);
        LOGGER.debug("Using preview measurements {}", previewMeasurements);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = previewMeasurements.sampleSize;
        final Bitmap original =
                rotateBitmap(BitmapFactory.decodeFile(file.getAbsolutePath(), options), rotation);
        if (original == null) {
            throw new IllegalArgumentException(String.format("Could not decode %s", mediaType));
        }
        return cropToMeasurements(original, previewMeasurements);
    }

    protected static Bitmap cropToMeasurements(
            final Bitmap original, final PreviewMeasurements previewMeasurements) {
        final Bitmap preview =
                Bitmap.createBitmap(
                        original,
                        previewMeasurements.x,
                        previewMeasurements.y,
                        previewMeasurements.width,
                        previewMeasurements.height);
        LOGGER.info("Preview size {} bytes", preview.getByteCount());
        if (original != preview) {
            original.recycle();
        }
        return preview;
    }

    private static int getRotation(final File file) {
        final ExifInterface exif;
        try {
            exif = new ExifInterface(file);
        } catch (final IOException e) {
            return 0;
        }
        final int orientation =
                exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }

    private static Bitmap rotateBitmap(@NonNull Bitmap originalBitmap, final int degree) {
        if (degree == 0) {
            return originalBitmap;
        }
        final int width = originalBitmap.getWidth();
        final int height = originalBitmap.getHeight();
        final Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        final Bitmap rotatedBitmap =
                Bitmap.createBitmap(originalBitmap, 0, 0, width, height, matrix, true);
        originalBitmap.recycle();
        return rotatedBitmap;
    }
}
