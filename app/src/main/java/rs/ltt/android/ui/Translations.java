package rs.ltt.android.ui;

import android.content.Context;
import androidx.annotation.StringRes;
import rs.ltt.android.R;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.mua.util.Label;

public class Translations {

    public static String asHumanReadableName(final Context context, final Label label) {
        final Role role = label.getRole();
        if (role != null) {
            return context.getString(translate(role));
        } else {
            return label.getName();
        }
    }

    public static @StringRes int translate(final Role role) {
        switch (role) {
            case ALL:
                return R.string.role_name_all;
            case INBOX:
                return R.string.role_name_inbox;
            case ARCHIVE:
                return R.string.role_name_archive;
            case DRAFTS:
                return R.string.role_name_drafts;
            case FLAGGED:
                return R.string.role_name_flagged;
            case IMPORTANT:
                return R.string.role_name_important;
            case SENT:
                return R.string.role_name_sent;
            case TRASH:
                return R.string.role_name_trash;
            case JUNK:
                return R.string.role_name_junk;
            case SUBSCRIBED:
                return R.string.role_name_subscribed;
            default:
                throw new IllegalArgumentException(String.format("%s is not a known role", role));
        }
    }
}
