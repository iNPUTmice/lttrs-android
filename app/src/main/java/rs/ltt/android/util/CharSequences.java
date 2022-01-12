package rs.ltt.android.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CharSequences {

    public static final String EMPTY_STRING = "";

    @NonNull
    public static String nullToEmpty(@Nullable final CharSequence charSequence) {
        return charSequence == null ? EMPTY_STRING : charSequence.toString();
    }

    public static boolean isNullOrEmpty(final @Nullable CharSequence charSequence) {
        return charSequence == null || charSequence.length() == 0;
    }
}
