package rs.ltt.android.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;
import java.util.List;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import rs.ltt.android.cache.LocalAttachment;
import rs.ltt.android.util.AttachmentSerializer;
import rs.ltt.jmap.common.entity.Attachment;
import rs.ltt.jmap.common.entity.EmailBodyPart;

public class AttachmentSerializationTest {

    @Test
    public void serializeDeserialize() {
        final List<Attachment> attachments =
                ImmutableList.of(
                        EmailBodyPart.builder()
                                .blobId("1")
                                .type("image/png")
                                .name("hello.png")
                                .size(23L)
                                .build(),
                        EmailBodyPart.builder()
                                .blobId("2")
                                .type("text/plain")
                                .name("hello.txt")
                                .size(42L)
                                .build());
        final byte[] bytes = AttachmentSerializer.toByteArray(attachments);
        final List<Attachment> deserializedAttachments = AttachmentSerializer.of(bytes);
        Assert.assertEquals(EmailBodyPart.class, deserializedAttachments.get(0).getClass());
        Assert.assertEquals("1", deserializedAttachments.get(0).getBlobId());
        Assert.assertEquals("image/png", deserializedAttachments.get(0).getType());
        Assert.assertEquals("2", deserializedAttachments.get(1).getBlobId());
        Assert.assertEquals("text/plain", deserializedAttachments.get(1).getType());
    }

    @Test
    public void localAttachment() {
        final UUID uuid = UUID.randomUUID();
        final List<Attachment> attachments =
                ImmutableList.of(new LocalAttachment(uuid, MediaType.PNG, "test.png", 23));
        final byte[] bytes = AttachmentSerializer.toByteArray(attachments);
        final List<Attachment> deserializedAttachments = AttachmentSerializer.of(bytes);
        final LocalAttachment localAttachment = (LocalAttachment) deserializedAttachments.get(0);
        Assert.assertEquals(uuid, localAttachment.getUuid());
    }
}
