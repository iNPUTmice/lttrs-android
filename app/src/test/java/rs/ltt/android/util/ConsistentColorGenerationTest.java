package rs.ltt.android.util;

import org.junit.Assert;
import org.junit.Test;

public class ConsistentColorGenerationTest {

    @Test
    public void samIsKeppel() {
        Assert.assertEquals(0xff389999, ConsistentColorGeneration.rgbFromKey("sam@example.com"));
    }

    @Test
    public void ashIsLochinvar() {
        Assert.assertEquals(0xff379a8f, ConsistentColorGeneration.rgbFromKey("ash@example.com"));
    }

    @Test
    public void frankieIsBostonBlue() {
        Assert.assertEquals(
                0xff3998A1, ConsistentColorGeneration.rgbFromKey("frankie@example.com"));
    }

    @Test
    public void harperIsPortage() {
        Assert.assertEquals(0xffa173ed, ConsistentColorGeneration.rgbFromKey("harper@example.com"));
    }

    @Test
    public void jordanIsCopper() {
        Assert.assertEquals(0xffcb7634, ConsistentColorGeneration.rgbFromKey("jordan@example.com"));
    }

    @Test
    public void kaneIsLuxorGold() {
        Assert.assertEquals(0xffab8634, ConsistentColorGeneration.rgbFromKey("kane@example.com"));
    }

    @Test
    public void maxIsSeaGreen() {
        Assert.assertEquals(0xff349e5b, ConsistentColorGeneration.rgbFromKey("max@example.com"));
    }

    @Test
    public void quinIsChateauGreen() {
        Assert.assertEquals(0xff359e5c, ConsistentColorGeneration.rgbFromKey("quin@example.com"));
    }

    @Test
    public void rileyIsMediumPurple() {
        Assert.assertEquals(0xffb16bed, ConsistentColorGeneration.rgbFromKey("riley@example.com"));
    }

    @Test
    public void xuIsBrilliantRose() {
        Assert.assertEquals(0xfff044b4, ConsistentColorGeneration.rgbFromKey("xu@example.com"));
    }
}
