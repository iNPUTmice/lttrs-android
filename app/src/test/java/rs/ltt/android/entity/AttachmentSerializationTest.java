package rs.ltt.android.entity;

import com.google.common.collect.ImmutableList;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.List;


public class AttachmentSerializationTest {

    @Test
    public void serializeDeserialize() {
        final List<Attachment> attachments = ImmutableList.of(
                new Attachment("1", "image/png", "hello.png", 23),
                new Attachment("2", "text/plain", "hello.txt", 42)
        );
        final byte[] bytes = rs.ltt.android.entity.Attachment.toByteArray(attachments);
        final Collection<rs.ltt.jmap.common.entity.Attachment> deserializedAttachments = Attachment.of(bytes);
        Assert.assertEquals(attachments, deserializedAttachments);
    }

}
