package rs.ltt.android.entity;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Preview preview1 = (Preview) o;
        return isEncrypted == preview1.isEncrypted && Objects.equal(preview, preview1.preview);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(preview, isEncrypted);
    }
}
