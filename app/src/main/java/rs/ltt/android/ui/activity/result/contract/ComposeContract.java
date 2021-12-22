package rs.ltt.android.ui.activity.result.contract;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rs.ltt.android.ui.activity.ComposeActivity;

public class ComposeContract extends ActivityResultContract<Bundle, Bundle> {
    @NonNull
    @Override
    public Intent createIntent(@NonNull final Context context, final Bundle extras) {
        final Intent intent = new Intent(context, ComposeActivity.class);
        intent.putExtras(extras);
        return intent;
    }

    @Override
    public Bundle parseResult(int resultCode, @Nullable Intent result) {
        if (resultCode != Activity.RESULT_OK || result == null) {
            return null;
        }
        return result.getExtras();
    }
}
