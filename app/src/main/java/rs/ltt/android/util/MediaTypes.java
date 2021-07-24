package rs.ltt.android.util;

import com.google.common.net.MediaType;

@SuppressWarnings("UnstableApiUsage")
public final class MediaTypes {

    private static final MediaType X_VCALENDAR = MediaType.create("text", "x-vcalendar");
    private static final MediaType CALENDAR = MediaType.create("text", "calendar");
    private static final MediaType RAR = MediaType.create("application", "rar");
    private static final MediaType VCARD = MediaType.create("text", "x-vcard");
    private static final MediaType MOBI = MediaType.create("application", "vnd.amazon.mobi8-ebook");
    private static final MediaType TEX = MediaType.create("text", "x-tex");
    private static final MediaType PLAIN = MediaType.create("text", "plain");
    private static final MediaType GPX_XML = MediaType.create("application", "gpx+xml");

    private MediaTypes() {

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

    public static boolean isDocument(final MediaType mediaType) {
        return mediaType.is(MediaType.OPENDOCUMENT_TEXT)
                || mediaType.is(MediaType.OOXML_DOCUMENT)
                || mediaType.is(MediaType.MICROSOFT_WORD)
                || mediaType.is(TEX)
                || mediaType.is(PLAIN);
    }

    public static boolean isTour(final MediaType mediaType) {
        return mediaType.is(GPX_XML)
                || mediaType.is(MediaType.GEO_JSON)
                || mediaType.is(MediaType.KML);
    }

    public static String toString(final MediaType mediaType) {
        return mediaType == null ? "*/*" : String.format("%s/%s", mediaType.type(), mediaType.subtype());
    }

}
