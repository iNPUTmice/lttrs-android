package rs.ltt.android.entity;

import com.google.common.base.MoreObjects;

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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("blobId", blobId)
                .add("type", type)
                .add("name", name)
                .add("size", size)
                .toString();
    }
}
