package rs.ltt.android.entity;

import com.google.common.net.MediaType;

import rs.ltt.android.util.MediaTypes;
import rs.ltt.jmap.common.entity.Downloadable;

public interface Attachment extends Downloadable {

    String getName();

    String getCharset();

    default MediaType getMediaType() {
        return MediaTypes.of(getType(), getCharset());
    }
}
