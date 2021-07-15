package rs.ltt.android.util;

import com.google.common.net.MediaType;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

@SuppressWarnings("UnstableApiUsage")
public final class MediaTypes {

    private static final MediaType X_VCALENDAR = MediaType.create("text","x-vcalendar");
    private static final MediaType CALENDAR = MediaType.create("text","calendar");
    private static final MediaType RAR = MediaType.create("application","rar");
    private static final MediaType VCARD = MediaType.create("text","x-vcard");
    private static final MediaType MOBI = MediaType.create("application","vnd.amazon.mobi8-ebook");

    private MediaTypes() {

    }

    public static MediaType of(final String type, final String charsetName) {
        final MediaType mediaType = type == null ? null : MediaType.parse(type);
        final Charset charset = parseCharset(charsetName);
        if (mediaType != null && charset != null) {
            return mediaType.withCharset(charset);
        }
        return mediaType;
    }

    private static Charset parseCharset(final String charset) {
        try {
            return charset == null ? null : Charset.forName(charset);
        } catch (final UnsupportedCharsetException e) {
            return null;
        }
    }

    public static boolean isCalendar(final MediaType mediaType) {
        return mediaType.is(X_VCALENDAR) || mediaType.is(CALENDAR);
    }

    public static boolean isArchive(final MediaType mediaType) {
        return mediaType.is(MediaType.ZIP) || mediaType.is(MediaType.TAR) || mediaType.is(RAR);
    }

    public static boolean isVcard(final MediaType mediaType) {
        return mediaType.is(VCARD);
    }

    public static boolean isEbook(final MediaType mediaType) {
        return mediaType.is(MediaType.EPUB) || mediaType.is(MOBI);
    }

}
