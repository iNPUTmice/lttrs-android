package rs.ltt.android.worker;

import android.app.NotificationManager;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkerParameters;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.google.common.net.MediaType;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.RateLimiter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.cache.BlobStorage;
import rs.ltt.android.cache.LocalAttachment;
import rs.ltt.android.ui.notification.AttachmentNotification;
import rs.ltt.jmap.client.blob.LegacyFileUpload;
import rs.ltt.jmap.client.blob.Progress;
import rs.ltt.jmap.common.entity.Attachment;
import rs.ltt.jmap.common.entity.EmailBodyPart;
import rs.ltt.jmap.common.entity.Upload;
import rs.ltt.jmap.mua.Mua;

public class BlobUploadWorker extends AbstractMuaWorker implements Progress {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlobUploadWorker.class);
    public static final String BLOB_ID_KEY = "blobId";
    private static final String NAME_KEY = "name";
    private static final String TYPE_KEY = "type";
    private static final String SIZE_KEY = "size";
    private static final String LOCAL_ATTACHMENT_UUID = "localAttachmentId";
    private final NotificationManager notificationManager;
    private final RateLimiter notificationRateLimiter = RateLimiter.create(1);
    private final LocalAttachment localAttachment;
    private int currentlyShownProgress = Integer.MIN_VALUE;
    private ListenableFuture<Upload> uploadFuture;

    public BlobUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = workerParams.getInputData();
        final UUID uuid =
                UUID.fromString(Objects.requireNonNull(data.getString(LOCAL_ATTACHMENT_UUID)));
        final String name = data.getString(NAME_KEY);
        final String type = Objects.requireNonNull(data.getString(TYPE_KEY));
        final long size = data.getLong(SIZE_KEY, 0);
        this.localAttachment = new LocalAttachment(uuid, MediaType.parse(type), name, size);
        this.notificationManager = context.getSystemService(NotificationManager.class);
    }

    public static Data data(Long accountId, final LocalAttachment attachment) {
        return new Data.Builder()
                .putLong(ACCOUNT_KEY, accountId)
                .putString(LOCAL_ATTACHMENT_UUID, attachment.getUuid().toString())
                .putString(NAME_KEY, attachment.getName())
                .putString(TYPE_KEY, attachment.getType())
                .putLong(SIZE_KEY, attachment.getSize())
                .build();
    }

    public static String uniqueName() {
        return "blob-upload";
    }

    public static Attachment getAttachment(final Data data) {
        return EmailBodyPart.builder()
                .blobId(data.getString(BLOB_ID_KEY))
                .type(data.getString(TYPE_KEY))
                .name(data.getString(NAME_KEY))
                .size(data.getLong(SIZE_KEY, 0))
                .build();
    }

    private static boolean isDone(final ListenableFuture<?> future) {
        return future != null && future.isDone();
    }

    @NonNull
    @Override
    public Result doWork() {
        // begin to display notification even if we don’t run as ForegroundService on Android 12
        this.onProgress(0);
        final File file = LocalAttachment.asFile(getApplicationContext(), localAttachment);
        final Mua mua = getMua();
        try (final LegacyFileUpload fileUpload =
                LegacyFileUpload.of(file, localAttachment.getMediaType())) {
            this.uploadFuture = mua.upload(fileUpload, this);
            final Upload upload = this.uploadFuture.get();
            LOGGER.info("Upload succeeded {}", upload);
            notifyUploadComplete();
            cacheBlob(file, upload.getBlobId());
            LocalAttachment.delete(getApplicationContext(), localAttachment);
            final Data data =
                    new Data.Builder()
                            .putString(BLOB_ID_KEY, upload.getBlobId())
                            .putString(TYPE_KEY, upload.getType())
                            .putString(NAME_KEY, localAttachment.getName())
                            .putLong(SIZE_KEY, upload.getSize())
                            .build();
            return Result.success(data);
        } catch (final ExecutionException e) {
            LOGGER.info("Failure uploading blob (ee) ", e.getCause());
            return Result.failure(Failure.of(e.getCause()));
        } catch (final Exception e) {
            LOGGER.info("Failure uploading blob", e);
            return Result.failure(Failure.of(e));
        } finally {
            notificationManager.cancel(AttachmentNotification.UPLOAD_ID);
        }
    }

    private void cacheBlob(final File file, final String blobId) {
        final BlobStorage blobStorage = BlobStorage.get(getApplicationContext(), account, blobId);
        if (blobStorage.file.exists()) {
            LOGGER.info("Blob {} is already cached", blobId);
            return;
        }
        if (file.renameTo(blobStorage.file)) {
            LOGGER.info("Successfully cached blob {} by moving local attachment", blobId);
            return;
        }
        final long bytesCopied;
        try (final InputStream inputStream = new FileInputStream(file);
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

    @NonNull
    @Override
    public ListenableFuture<ForegroundInfo> getForegroundInfoAsync() {
        return Futures.immediateFuture(
                new ForegroundInfo(
                        AttachmentNotification.UPLOAD_ID,
                        AttachmentNotification.uploading(
                                getApplicationContext(),
                                getId(),
                                Strings.nullToEmpty(localAttachment.getName()),
                                0,
                                true)));
    }

    private void notifyUploadComplete() {
        notificationManager.notify(
                AttachmentNotification.UPLOAD_ID,
                AttachmentNotification.uploaded(
                        getApplicationContext(), Strings.nullToEmpty(localAttachment.getName())));
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
                            Strings.nullToEmpty(localAttachment.getName()),
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
}
