package rs.ltt.android.entity;

import rs.ltt.jmap.common.entity.Downloadable;

public class DownloadableBlob implements Downloadable {

    private final String blobId;
    private final String type;
    private final String name;

    public DownloadableBlob(String blobId, String type, String name) {
        this.blobId = blobId;
        this.type = type;
        this.name = name;
    }

    @Override
    public String getBlobId() {
        return this.blobId;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
