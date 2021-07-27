package rs.ltt.android.ui.notification;

import org.junit.Assert;
import org.junit.Test;

public class EmailNotificationTagTest {

    @Test(expected = IllegalArgumentException.class)
    public void parseNoAccount() {
        EmailNotification.Tag.parse("test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseEmptyAccount() {
        EmailNotification.Tag.parse("-test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseEmptyEmailId() {
        EmailNotification.Tag.parse("0-");
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidAccountId() {
        EmailNotification.Tag.parse("test-test");
    }

    @Test
    public void validTag() {
        final EmailNotification.Tag tag = EmailNotification.Tag.parse("0-test");
        Assert.assertEquals(0L, tag.getAccountId());
        Assert.assertEquals("test", tag.getEmailId());
    }

    @Test
    public void print() {
        final EmailNotification.Tag tag = new EmailNotification.Tag(0L, "test");
        Assert.assertEquals("0-test", tag.toString());
    }
}
