package rs.ltt.android.util;

import com.google.common.collect.ImmutableList;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import rs.ltt.jmap.common.entity.Attachment;

public class AttachmentSerializationTest {

    @Test
    public void serializeDeserialize() throws IOException {
        final List<Attachment> attachments = ImmutableList.of(
                new rs.ltt.android.entity.Attachment("1","image/png","hello.png",23),
                new rs.ltt.android.entity.Attachment("2","text/plain","hello.txt", 42)
        );
        final byte[] bytes = rs.ltt.android.entity.Attachment.toByteArray(attachments);
        final Collection<Attachment> deserializedAttachments = rs.ltt.android.entity.Attachment.of(bytes);
        Assert.assertEquals(attachments, deserializedAttachments);
    }

}
