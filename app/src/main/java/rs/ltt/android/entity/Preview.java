package rs.ltt.android.entity;

import com.google.common.base.MoreObjects;

public class Preview {
    private final String preview;
    public final boolean isEncrypted;

    public Preview(final String preview, final boolean isEncrypted) {
        this.preview = preview;
        this.isEncrypted = isEncrypted;
    }

    public String getPreview() {
        return preview == null ? null : preview.trim();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("preview", preview)
                .add("isEncrypted", isEncrypted)
                .toString();
    }
}
