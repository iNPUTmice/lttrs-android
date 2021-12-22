package rs.ltt.android.util;

public final class FileSizes {

    private FileSizes() {}

    public static String toString(final long size) {
        if (size >= (1024 * 1024 * 1024)) {
            return Math.round(size * 1f / (1024 * 1024 * 1024)) + " GiB";
        } else if (size > (1.5 * 1024 * 1024)) {
            return Math.round(size * 1f / (1024 * 1024)) + " MiB";
        } else if (size >= 1024) {
            return Math.round(size * 1f / 1024) + " KiB";
        } else {
            return size + " B";
        }
    }
}
