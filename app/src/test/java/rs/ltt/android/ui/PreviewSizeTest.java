package rs.ltt.android.ui;

import org.junit.Assert;
import org.junit.Test;
import rs.ltt.android.ui.preview.Size;

public class PreviewSizeTest {

    @Test
    public void upscalePortraitPdf() {
        final Size previewSize = new Size(726, 264);
        final Size pageSize = new Size(210, 297);
        final Size renderingSize = pageSize.scaleTo(previewSize);
        Assert.assertEquals(840, renderingSize.width);
        Assert.assertEquals(1188, renderingSize.height);
    }

    @Test
    public void upscaleLandscapePdf() {
        final Size previewSize = new Size(726, 264);
        final Size pageSize = new Size(297, 210);
        final Size renderingSize = pageSize.scaleTo(previewSize);
        Assert.assertEquals(891, renderingSize.width);
        Assert.assertEquals(630, renderingSize.height);
    }

    @Test
    public void upscaleVeryLandscapePdf() {
        final Size previewSize = new Size(726, 264);
        final Size pageSize = new Size(200, 10);
        final Size renderingSize = pageSize.scaleTo(previewSize);
        Assert.assertEquals(800, renderingSize.width);
        Assert.assertEquals(40, renderingSize.height);
    }

    @Test
    public void downscalePortraitPdf() {
        final Size previewSize = new Size(726, 264);
        final Size pageSize = new Size(2100, 2970);
        final Size renderingSize = pageSize.scaleTo(previewSize);
        Assert.assertEquals(1050, renderingSize.width);
        Assert.assertEquals(1485, renderingSize.height);
    }

    @Test
    public void downscaleLandscapePdf() {
        final Size previewSize = new Size(726, 264);
        final Size pageSize = new Size(2970, 2100);
        final Size renderingSize = pageSize.scaleTo(previewSize);
        Assert.assertEquals(742, renderingSize.width);
        Assert.assertEquals(525, renderingSize.height);
    }
}
