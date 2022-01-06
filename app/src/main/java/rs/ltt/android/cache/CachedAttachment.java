package rs.ltt.android.cache;

import com.google.common.base.CharMatcher;
import java.io.File;

public abstract class CachedAttachment {

    public abstract File getFile();

    protected static String cleanId(final String id) {
        return CharMatcher.is(File.pathSeparatorChar).removeFrom(id);
    }

    public static class InvalidCacheException extends IllegalStateException {

        InvalidCacheException(final File file) {
            super(String.format("%s not found", file.getAbsolutePath()));
        }
    }
}
