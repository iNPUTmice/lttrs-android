package rs.ltt.android.ui.preview;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.ui.PreviewMeasurements;

public class PdfDocumentPreview extends ImagePreview {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfDocumentPreview.class);

    private PdfDocumentPreview() {}

    public static Bitmap getPdfDocumentPreview(final File file, final Size previewSize) {
        try (final PdfRenderer pdfRenderer =
                new PdfRenderer(
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))) {
            try (final PdfRenderer.Page page = pdfRenderer.openPage(0)) {
                final Size pageSize =
                        new Size(page.getWidth(), page.getHeight()).scaleTo(previewSize);
                final int width = pageSize.width;
                final int height = pageSize.height;
                final PreviewMeasurements previewMeasurements =
                        PreviewMeasurements.of(width, height, previewSize);
                LOGGER.debug(
                        "width={} ({}), height={} ({}), previewMeasurements={}",
                        width,
                        page.getWidth(),
                        height,
                        page.getHeight(),
                        previewMeasurements);
                final Bitmap rendered = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                rendered.eraseColor(0xffffffff);
                page.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                return cropToMeasurements(rendered, previewMeasurements);
            }
        } catch (final IOException e) {
            throw new IllegalStateException("Could not generate preview for PDF", e);
        }
    }
}
