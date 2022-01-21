package rs.ltt.android.ui;

import org.junit.Assert;
import org.junit.Test;

public class PreviewMeasurementsTest {

    @Test
    public void landscape2048x1536PreviewOfSameRatio() {
        final PreviewMeasurements previewMeasurements =
                PreviewMeasurements.of(2048, 1536, 512, 384);
        Assert.assertEquals(4, previewMeasurements.sampleSize);
        Assert.assertEquals(0, previewMeasurements.x);
        Assert.assertEquals(0, previewMeasurements.y);
        Assert.assertEquals(512, previewMeasurements.width);
        Assert.assertEquals(384, previewMeasurements.height);
    }

    @Test
    public void landscape2048x1536PreviewOf169() {
        final PreviewMeasurements previewMeasurements = PreviewMeasurements.of(2048, 1536, 160, 90);
        Assert.assertEquals(8, previewMeasurements.sampleSize);
        Assert.assertEquals(0, previewMeasurements.x);
        Assert.assertEquals(24, previewMeasurements.y);
        Assert.assertEquals(256, previewMeasurements.width);
        Assert.assertEquals(144, previewMeasurements.height);
    }

    @Test
    public void landscape2048x1536PreviewOf169Large() {
        final PreviewMeasurements previewMeasurements =
                PreviewMeasurements.of(2048, 1536, 1600, 900);
        Assert.assertEquals(1, previewMeasurements.sampleSize);
        Assert.assertEquals(0, previewMeasurements.x);
        Assert.assertEquals(192, previewMeasurements.y);
        Assert.assertEquals(2048, previewMeasurements.width);
        Assert.assertEquals(1152, previewMeasurements.height);
    }

    @Test
    public void landscape2048x1PreviewOf169Large() {
        final PreviewMeasurements previewMeasurements = PreviewMeasurements.of(2048, 1, 1600, 900);

        Assert.assertEquals(1, previewMeasurements.sampleSize);

        Assert.assertEquals(224, previewMeasurements.x);

        Assert.assertEquals(1600, previewMeasurements.width);
        Assert.assertEquals(1, previewMeasurements.height);
    }

    @Test
    public void square2048x2048PreviewOf169() {
        final PreviewMeasurements previewMeasurements = PreviewMeasurements.of(2048, 2048, 160, 90);
        Assert.assertEquals(8, previewMeasurements.sampleSize);
        Assert.assertEquals(0, previewMeasurements.x);
        Assert.assertEquals(56, previewMeasurements.y);
        Assert.assertEquals(256, previewMeasurements.width);
        Assert.assertEquals(144, previewMeasurements.height);
    }

    @Test
    public void portrait1536x2048PreviewOf169() {
        final PreviewMeasurements previewMeasurements = PreviewMeasurements.of(1536, 2048, 160, 90);
        Assert.assertEquals(8, previewMeasurements.sampleSize);
        Assert.assertEquals(0, previewMeasurements.x);
        Assert.assertEquals(0, previewMeasurements.y);
        Assert.assertEquals(192, previewMeasurements.width);
        Assert.assertEquals(108, previewMeasurements.height);
    }

    @Test
    public void portrait480x90PreviewOf169() {
        final PreviewMeasurements previewMeasurements = PreviewMeasurements.of(480, 90, 160, 90);
        Assert.assertEquals(1, previewMeasurements.sampleSize);
        Assert.assertEquals(160, previewMeasurements.x);
        Assert.assertEquals(0, previewMeasurements.y);
        Assert.assertEquals(160, previewMeasurements.width);
        Assert.assertEquals(90, previewMeasurements.height);
    }

    @Test
    public void landscape480x80PreviewOf169() {
        final PreviewMeasurements previewMeasurements = PreviewMeasurements.of(480, 80, 160, 90);
        System.out.println(previewMeasurements);
        Assert.assertEquals(1, previewMeasurements.sampleSize);
        Assert.assertEquals(160, previewMeasurements.x);
        Assert.assertEquals(0, previewMeasurements.y);
        Assert.assertEquals(160, previewMeasurements.width);
        Assert.assertEquals(80, previewMeasurements.height);
    }

    @Test
    public void portrait1536x2048PreviewOf169Large() {
        final PreviewMeasurements previewMeasurements =
                PreviewMeasurements.of(1536, 2048, 1600, 900);
        Assert.assertEquals(1, previewMeasurements.sampleSize);
        Assert.assertEquals(0, previewMeasurements.x);
        Assert.assertEquals(0, previewMeasurements.y);
    }

    @Test
    public void portrait1x2048PreviewOf169Large() {
        final PreviewMeasurements previewMeasurements = PreviewMeasurements.of(1, 2048, 1600, 900);
        Assert.assertEquals(1, previewMeasurements.sampleSize);
        Assert.assertEquals(0, previewMeasurements.x);
        Assert.assertEquals(0, previewMeasurements.y);
        Assert.assertEquals(1, previewMeasurements.width);
        Assert.assertEquals(900, previewMeasurements.height);
    }

    @Test
    public void portrait1x2PreviewOf169Large() {
        final PreviewMeasurements previewMeasurements = PreviewMeasurements.of(1, 2, 1600, 900);
        Assert.assertEquals(1, previewMeasurements.sampleSize);
        Assert.assertEquals(0, previewMeasurements.x);
        Assert.assertEquals(0, previewMeasurements.y);
        Assert.assertEquals(1, previewMeasurements.width);
        Assert.assertEquals(2, previewMeasurements.height);
    }

    @Test
    public void realWorldScenarioOne() {
        final PreviewMeasurements previewMeasurements =
                PreviewMeasurements.of(4032, 3024, 726, 264);
        Assert.assertEquals(4, previewMeasurements.sampleSize);
        Assert.assertEquals(0, previewMeasurements.x);
        Assert.assertEquals(1008, previewMeasurements.width);
    }

    @Test
    public void realWordScenarioTwo() {
        final PreviewMeasurements previewMeasurements = PreviewMeasurements.of(2000, 20, 726, 264);
        Assert.assertEquals(1, previewMeasurements.sampleSize);
        Assert.assertEquals(0, previewMeasurements.y);
        Assert.assertEquals(20, previewMeasurements.height);
        Assert.assertEquals(726, previewMeasurements.width);
        System.out.println(previewMeasurements);
    }
}
