package rs.ltt.android.ui.preview;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import com.google.common.net.MediaType;
import java.io.File;
import rs.ltt.android.ui.PreviewMeasurements;

public class VideoPreview extends ImagePreview {
    public static Bitmap getVideoPreview(
            final File file, final MediaType mediaType, final Size size) {
        try (final MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever()) {
            metadataRetriever.setDataSource(file.getAbsolutePath());
            final Bitmap original = metadataRetriever.getFrameAtTime();
            final int width = original.getWidth();
            final int height = original.getHeight();
            final PreviewMeasurements previewMeasurements =
                    PreviewMeasurements.of(width, height, size);
            return cropToMeasurements(original, previewMeasurements);
        }
    }
}
