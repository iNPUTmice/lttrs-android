package rs.ltt.android.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import com.google.common.net.MediaType;
import rs.ltt.android.R;
import rs.ltt.android.util.MediaTypes;

public class ViewIntent {
    public final Uri uri;
    public final MediaType mediaType;

    public ViewIntent(Uri uri, MediaType mediaType) {
        this.uri = uri;
        this.mediaType = mediaType;
    }

    public void launch(final Activity activity) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, MediaTypes.toString(mediaType));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activity.startActivity(intent);
        } catch (final ActivityNotFoundException e) {
            MaterialAlertDialogs.error(
                    activity,
                    activity.getString(
                            R.string.no_application_to_open_x, MediaTypes.toString(mediaType)));
        }
    }
}
