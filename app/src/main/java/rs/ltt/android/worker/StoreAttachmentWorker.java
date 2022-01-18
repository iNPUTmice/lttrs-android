package rs.ltt.android.worker;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoreAttachmentWorker extends Worker {

    public static final String FILE_KEY = "file";
    public static final String TARGET_URI_KEY = "target";
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreAttachmentWorker.class);
    private final File file;
    private final Uri target;

    public StoreAttachmentWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = workerParams.getInputData();
        this.file = new File(Objects.requireNonNull(data.getString(FILE_KEY)));
        this.target = Uri.parse(Objects.requireNonNull(data.getString(TARGET_URI_KEY)));
    }

    public static Data data(final File file, final Uri uri) {
        return new Data.Builder()
                .putString(FILE_KEY, file.getAbsolutePath())
                .putString(TARGET_URI_KEY, uri.toString())
                .build();
    }

    public static Data data(final Uri uri) {
        return new Data.Builder().putString(TARGET_URI_KEY, uri.toString()).build();
    }

    @NonNull
    @Override
    public Result doWork() {
        LOGGER.info("copy {} to {}", file.getAbsolutePath(), target);
        final long bytesCopied;
        try (final InputStream inputStream = new FileInputStream(this.file);
                final OutputStream outputStream =
                        getApplicationContext()
                                .getContentResolver()
                                .openOutputStream(this.target)) {
            bytesCopied = ByteStreams.copy(inputStream, outputStream);
            outputStream.flush();
        } catch (final Exception e) {
            LOGGER.error("Unable to copy file", e);
            // TODO delete target?
            return Result.failure();
        }
        // TODO give up permission?
        LOGGER.info("Copied {} bytes", bytesCopied);
        return Result.success();
    }
}
