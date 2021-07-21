package rs.ltt.android.entity;

import androidx.annotation.NonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Attachment implements rs.ltt.jmap.common.entity.Attachment {

    private final String blobId;
    private final String type;
    private final String name;
    private final long size;

    public Attachment(String blobId, String type, String name, long size) {
        this.blobId = blobId;
        this.type = type;
        this.name = name;
        this.size = size;
    }

    @Override
    public String getBlobId() {
        return blobId;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCharset() {
        return null;
    }

    @Override
    public Long getSize() {
        return size;
    }

    @NotNull
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("blobId", blobId)
                .add("type", type)
                .add("name", name)
                .add("size", size)
                .toString();
    }

    public static List<rs.ltt.jmap.common.entity.Attachment> of(final byte[] bytes) {
        try {
            return ofThrow(bytes);
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private static List<rs.ltt.jmap.common.entity.Attachment> ofThrow(final byte[] bytes) throws IOException {
        final DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes));
        final int count = dataInputStream.readInt();
        if (count == 0) {
            return Collections.emptyList();
        }
        final ImmutableList.Builder<rs.ltt.jmap.common.entity.Attachment> builder = new ImmutableList.Builder<>();
        for (int i = 0; i < count; ++i) {
            builder.add(read(dataInputStream));
        }
        return builder.build();
    }

    private static @NonNull
    Attachment read(final DataInputStream dataInputStream) throws IOException {
        final String blobId = dataInputStream.readUTF();
        final String type = dataInputStream.readUTF();
        final String name = dataInputStream.readUTF();
        final long size = dataInputStream.readLong();
        return new Attachment(blobId, type, name, size);
    }

    public static byte[] toByteArray(final Collection<rs.ltt.jmap.common.entity.Attachment> attachments) {
        try {
            return toByteArrayThrows(attachments);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] toByteArrayThrows(final Collection<rs.ltt.jmap.common.entity.Attachment> attachments) throws IOException {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        if (attachments == null) {
            dataOutputStream.writeInt(0);
            return byteArrayOutputStream.toByteArray();
        }
        dataOutputStream.writeInt(attachments.size());
        for (final rs.ltt.jmap.common.entity.Attachment attachment : attachments) {
            write(dataOutputStream, attachment);
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static void write(DataOutputStream dataOutputStream, rs.ltt.jmap.common.entity.Attachment attachment) throws IOException {
        dataOutputStream.writeUTF(attachment.getBlobId());
        dataOutputStream.writeUTF(attachment.getType());
        dataOutputStream.writeUTF(attachment.getName());
        dataOutputStream.writeLong(attachment.getSize());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attachment that = (Attachment) o;
        return size == that.size &&
                Objects.equal(blobId, that.blobId) &&
                Objects.equal(type, that.type) &&
                Objects.equal(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(blobId, type, name, size);
    }
}
