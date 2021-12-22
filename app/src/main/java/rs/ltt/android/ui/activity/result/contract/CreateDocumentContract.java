package rs.ltt.android.ui.activity.result.contract;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rs.ltt.android.util.MediaTypes;
import rs.ltt.jmap.common.entity.Attachment;

public class CreateDocumentContract extends ActivityResultContract<Attachment, Uri> {

    @CallSuper
    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, @NonNull Attachment attachment) {
        return new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setType(MediaTypes.toString(attachment.getMediaType()))
                .putExtra(Intent.EXTRA_TITLE, attachment.getName());
    }

    @Nullable
    @Override
    public final SynchronousResult<Uri> getSynchronousResult(
            @NonNull Context context, @NonNull Attachment attachment) {
        return null;
    }

    @Nullable
    @Override
    public final Uri parseResult(int resultCode, @Nullable Intent intent) {
        if (intent == null || resultCode != Activity.RESULT_OK) return null;
        return intent.getData();
    }
}
