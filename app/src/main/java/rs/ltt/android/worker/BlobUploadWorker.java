package rs.ltt.android.worker;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkInfo;
import androidx.work.WorkerParameters;

import com.google.common.base.Preconditions;
import com.google.common.net.MediaType;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Objects;

import rs.ltt.android.entity.Attachment;
import rs.ltt.jmap.client.blob.Progress;
import rs.ltt.jmap.client.blob.Upload;
import rs.ltt.jmap.client.blob.Uploadable;
import rs.ltt.jmap.mua.Mua;

public class BlobUploadWorker extends AbstractMuaWorker implements Progress {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlobUploadWorker.class);
    private static final String URI_KEY = "uri";
    private static final String BLOB_ID_KEY = "blobId";
    private static final String NAME_KEY = "name";
    private static final String TYPE_KEY = "type";
    private static final String SIZE_KEY = "size";
    private final Uri uri;

    public BlobUploadWorker(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = workerParams.getInputData();
        this.uri = Uri.parse(Objects.requireNonNull(data.getString(URI_KEY)));
    }

    public static Data data(Long accountId, final Uri uri) {
        return new Data.Builder()
                .putLong(ACCOUNT_KEY, accountId)
                .putString(URI_KEY, uri.toString())
                .build();
    }

    public static String uniqueName() {
        return "blob-upload";
    }

    public static Attachment getAttachment(final WorkInfo workInfo) {
        Preconditions.checkState(
                workInfo.getState() == WorkInfo.State.SUCCEEDED,
                "Work must have succeeded to extract attachment"
        );
        final Data data = workInfo.getOutputData();
        return new Attachment(
                data.getString(BLOB_ID_KEY),
                data.getString(TYPE_KEY),
                data.getString(NAME_KEY),
                data.getLong(SIZE_KEY, 0)
        );
    }

    @NonNull
    @NotNull
    @Override
    public Result doWork() {
        //https://developer.android.com/training/secure-file-sharing/retrieve-info
        final ContentResolver contentResolver = getApplicationContext().getContentResolver();
        final String type = contentResolver.getType(uri);
        final long size;
        final String name;
        try (final Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
            cursor.moveToFirst();
            size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
            name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
        } catch (final Exception e) {
            LOGGER.debug("Unable to retrieve file size and name", e);
            return Result.failure();
        }
        final Mua mua = getMua();
        try (final InputStream inputStream = contentResolver.openInputStream(uri)) {
            final Uploadable uploadable = new ContentProviderUpload(
                    inputStream,
                    MediaType.parse(type),
                    size
            );
            final Upload upload = mua.upload(uploadable, this).get();
            LOGGER.info("Upload succeeded {}", upload);
            final Data data = new Data.Builder()
                    .putString(BLOB_ID_KEY, upload.getBlobId())
                    .putString(TYPE_KEY, upload.getType())
                    .putString(NAME_KEY, name)
                    .putLong(SIZE_KEY, upload.getSize())
                    .build();
            return Result.success(data);
        } catch (final Exception e) {
            LOGGER.info("Failure uploading blob", e);
            return Result.failure();
        }
    }

    @Override
    public void onProgress(int progress) {
        LOGGER.debug("progress {}", progress);
    }

    private static class ContentProviderUpload implements Uploadable {

        private final InputStream inputStream;
        private final MediaType mediaType;
        private final long contentLength;

        private ContentProviderUpload(InputStream inputStream, MediaType mediaType, long size) {
            this.inputStream = inputStream;
            this.mediaType = mediaType;
            this.contentLength = size;
        }

        @Override
        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public MediaType getMediaType() {
            return mediaType;
        }

        @Override
        public long getContentLength() {
            return contentLength;
        }
    }
}
