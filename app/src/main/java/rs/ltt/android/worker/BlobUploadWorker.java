package rs.ltt.android.worker;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkInfo;
import androidx.work.WorkerParameters;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.google.common.net.MediaType;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.RateLimiter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.cache.BlobStorage;
import rs.ltt.android.entity.Attachment;
import rs.ltt.android.ui.notification.AttachmentNotification;
import rs.ltt.jmap.client.blob.Progress;
import rs.ltt.jmap.client.blob.Uploadable;
import rs.ltt.jmap.common.entity.Upload;
import rs.ltt.jmap.mua.Mua;

public class BlobUploadWorker extends AbstractMuaWorker implements Progress {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlobUploadWorker.class);
    private static final String URI_KEY = "uri";
    private static final String BLOB_ID_KEY = "blobId";
    private static final String NAME_KEY = "name";
    private static final String TYPE_KEY = "type";
    private static final String SIZE_KEY = "size";
    private final Uri uri;
    private final NotificationManager notificationManager;
    private final RateLimiter notificationRateLimiter = RateLimiter.create(1);
    private String name;
    private int currentlyShownProgress = 0;
    private ListenableFuture<Upload> uploadFuture;

    public BlobUploadWorker(
            @NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = workerParams.getInputData();
        this.uri = Uri.parse(Objects.requireNonNull(data.getString(URI_KEY)));
        this.notificationManager = context.getSystemService(NotificationManager.class);
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
                "Work must have succeeded to extract attachment");
        final Data data = workInfo.getOutputData();
        return new Attachment(
                data.getString(BLOB_ID_KEY),
                data.getString(TYPE_KEY),
                data.getString(NAME_KEY),
                data.getLong(SIZE_KEY, 0));
    }

    private static boolean isDone(final ListenableFuture<?> future) {
        return future != null && future.isDone();
    }

    @NonNull
    @NotNull
    @Override
    public Result doWork() {
        // https://developer.android.com/training/secure-file-sharing/retrieve-info
        final ContentResolver contentResolver = getApplicationContext().getContentResolver();
        final String type = contentResolver.getType(uri);
        final long size;
        try (final Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
            cursor.moveToFirst();
            size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
            name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
        } catch (final Exception e) {
            LOGGER.debug("Unable to retrieve file size and name", e);
            return Result.failure();
        }
        setForegroundAsync(getForegroundInfo());
        final Mua mua = getMua();
        try (final InputStream inputStream = contentResolver.openInputStream(uri)) {
            final Uploadable uploadable =
                    new ContentProviderUpload(inputStream, MediaType.parse(type), size);
            this.uploadFuture = mua.upload(uploadable, this);
            final Upload upload = this.uploadFuture.get();
            LOGGER.info("Upload succeeded {}", upload);
            notifyUploadComplete();
            cacheBlob(uri, upload.getBlobId());
            final Data data =
                    new Data.Builder()
                            .putString(BLOB_ID_KEY, upload.getBlobId())
                            .putString(TYPE_KEY, upload.getType())
                            .putString(NAME_KEY, name)
                            .putLong(SIZE_KEY, upload.getSize())
                            .build();
            return Result.success(data);
        } catch (final ExecutionException e) {
            LOGGER.info("Failure uploading blob (ee) ", e.getCause());
            return Result.failure(Failure.of(e.getCause()));
        } catch (final Exception e) {
            LOGGER.info("Failure uploading blob", e);
            return Result.failure(Failure.of(e));
        }
    }

    private void cacheBlob(final Uri uri, final String blobId) {
        final BlobStorage blobStorage = BlobStorage.get(getApplicationContext(), account, blobId);
        if (blobStorage.file.exists()) {
            LOGGER.info("Blob {} is already cached", blobId);
            return;
        }
        final ContentResolver contentResolver = getApplicationContext().getContentResolver();
        final long bytesCopied;
        try (final InputStream inputStream = contentResolver.openInputStream(uri);
                final FileOutputStream fileOutputStream =
                        new FileOutputStream(blobStorage.temporaryFile)) {
            bytesCopied = ByteStreams.copy(inputStream, fileOutputStream);
            fileOutputStream.flush();
        } catch (final Exception e) {
            LOGGER.warn("Unable to write InputStream to blob cache", e);
            if (blobStorage.temporaryFile.delete()) {
                LOGGER.info("Deleted temporary file");
            }
            return;
        }
        if (blobStorage.moveTemporaryToFile()) {
            LOGGER.info("Successfully cached blob {}. {} bytes written", blobId, bytesCopied);
        }
    }

    private ForegroundInfo getForegroundInfo() {
        return new ForegroundInfo(
                AttachmentNotification.UPLOAD_ID,
                AttachmentNotification.uploading(
                        getApplicationContext(), getId(), Strings.nullToEmpty(this.name), 0, true));
    }

    private void notifyUploadComplete() {
        notificationManager.notify(
                AttachmentNotification.UPLOAD_ID,
                AttachmentNotification.uploaded(
                        getApplicationContext(), Strings.nullToEmpty(this.name)));
    }

    @Override
    public void onProgress(int progress) {
        if (isDone(this.uploadFuture) || currentlyShownProgress == progress) {
            return;
        }
        if (notificationRateLimiter.tryAcquire()) {
            notificationManager.notify(
                    AttachmentNotification.UPLOAD_ID,
                    AttachmentNotification.uploading(
                            getApplicationContext(),
                            getId(),
                            Strings.nullToEmpty(this.name),
                            progress,
                            false));
            this.currentlyShownProgress = progress;
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        if (this.uploadFuture != null) {
            if (this.uploadFuture.cancel(true)) {
                LOGGER.info("Cancelled upload future");
            }
        }
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
