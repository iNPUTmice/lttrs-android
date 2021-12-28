package rs.ltt.android.util;

import androidx.annotation.NonNull;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import rs.ltt.android.cache.LocalAttachment;
import rs.ltt.jmap.common.entity.Attachment;
import rs.ltt.jmap.common.entity.EmailBodyPart;

public class AttachmentSerializer {

    private static final int ATTACHMENT_TYPE_BODY_PART = 1;
    private static final int ATTACHMENT_TYPE_LOCAL = 2;

    public static List<Attachment> of(final byte[] bytes) {
        try {
            return ofThrow(bytes);
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private static List<Attachment> ofThrow(final byte[] bytes) throws IOException {
        final DataInputStream dataInputStream =
                new DataInputStream(new ByteArrayInputStream(bytes));
        final int count = dataInputStream.readInt();
        if (count == 0) {
            return Collections.emptyList();
        }
        final ImmutableList.Builder<Attachment> builder = new ImmutableList.Builder<>();
        for (int i = 0; i < count; ++i) {
            builder.add(read(dataInputStream));
        }
        return builder.build();
    }

    private static @NonNull Attachment read(final DataInputStream dataInputStream)
            throws IOException {
        final int attachmentType = dataInputStream.readInt();
        final Object id;
        if (attachmentType == ATTACHMENT_TYPE_BODY_PART) {
            id = dataInputStream.readUTF();
        } else if (attachmentType == ATTACHMENT_TYPE_LOCAL) {
            final byte[] bytes = new byte[16];
            dataInputStream.readFully(bytes);
            id = bytesToUuid(bytes);
        } else {
            throw new IOException("Trying to read unknown attachment type");
        }
        final String type = dataInputStream.readUTF();
        final String name = dataInputStream.readUTF();
        final long size = dataInputStream.readLong();
        if (attachmentType == ATTACHMENT_TYPE_BODY_PART) {
            return EmailBodyPart.builder()
                    .blobId((String) id)
                    .type(type)
                    .name(name)
                    .size(size)
                    .build();
        } else {
            return new LocalAttachment((UUID) id, MediaType.parse(type), name, size);
        }
    }

    private static UUID bytesToUuid(final byte[] bytes) {
        Preconditions.checkArgument(bytes.length == 16, "Provide 16 bytes for UUID");
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }

    public static byte[] uuidToBytes(final UUID uuid) {
        return ByteBuffer.wrap(new byte[16])
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();
    }

    public static byte[] toByteArray(final Collection<? extends Attachment> attachments) {
        try {
            return toByteArrayThrows(attachments);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] toByteArrayThrows(final Collection<? extends Attachment> attachments)
            throws IOException {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        if (attachments == null) {
            dataOutputStream.writeInt(0);
            return byteArrayOutputStream.toByteArray();
        }
        dataOutputStream.writeInt(attachments.size());
        for (final Attachment attachment : attachments) {
            write(dataOutputStream, attachment);
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static void write(final DataOutputStream dataOutputStream, final Attachment attachment)
            throws IOException {
        if (attachment instanceof LocalAttachment) {
            final LocalAttachment localAttachment = (LocalAttachment) attachment;
            dataOutputStream.writeInt(ATTACHMENT_TYPE_LOCAL);
            dataOutputStream.write(uuidToBytes(localAttachment.getUuid()));
        } else {
            dataOutputStream.writeInt(ATTACHMENT_TYPE_BODY_PART);
            dataOutputStream.writeUTF(attachment.getBlobId());
        }
        dataOutputStream.writeUTF(attachment.getType());
        dataOutputStream.writeUTF(attachment.getName());
        dataOutputStream.writeLong(attachment.getSize());
    }
}
