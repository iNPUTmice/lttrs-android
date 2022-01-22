package rs.ltt.android.worker;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkInfo;
import androidx.work.WorkerParameters;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.RateLimiter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import okhttp3.Call;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.cache.BlobStorage;
import rs.ltt.android.entity.DownloadableBlob;
import rs.ltt.android.entity.EmailWithEncryptionStatus;
import rs.ltt.android.entity.EncryptionStatus;
import rs.ltt.android.ui.notification.AttachmentNotification;
import rs.ltt.autocrypt.jmap.AutocryptPlugin;
import rs.ltt.autocrypt.jmap.EncryptedBodyPart;
import rs.ltt.jmap.client.blob.Download;
import rs.ltt.jmap.client.blob.Progress;
import rs.ltt.jmap.client.io.ByteStreams;
import rs.ltt.jmap.common.entity.Downloadable;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.mua.Mua;

public class BlobDownloadWorker extends AbstractMuaWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlobDownloadWorker.class);

    private static final String EMAIL_ID_KEY = "emailId";
    private static final String BLOB_ID_KEY = "blobId";
    private static final String URI_KEY = "uri";

    private final String emailId;
    private final String blobId;
    private final NotificationManager notificationManager;
    private final RateLimiter notificationRateLimiter = RateLimiter.create(1);
    private final ListenableFuture<DownloadableBlob> downloadable;
    private Call call;
    private ListenableFuture<?> cancelableFuture;
    private int currentlyShownProgress = Integer.MIN_VALUE;

    public BlobDownloadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = workerParams.getInputData();
        this.emailId = data.getString(EMAIL_ID_KEY);
        this.blobId = data.getString(BLOB_ID_KEY);
        this.notificationManager = context.getSystemService(NotificationManager.class);
        this.downloadable = getDatabase().threadAndEmailDao().getDownloadable(emailId, blobId);
    }

    public static Uri getUri(final WorkInfo workInfo) {
        Preconditions.checkState(
                workInfo.getState() == WorkInfo.State.SUCCEEDED,
                "Work must have succeeded to extract uri");
        final Data data = workInfo.getOutputData();
        final String uri =
                Preconditions.checkNotNull(data.getString(URI_KEY), "OutputData is missing URI");
        return Uri.parse(uri);
    }

    public static Data data(final Long account, final String emailId, final String blobId) {
        return new Data.Builder()
                .putLong(ACCOUNT_KEY, account)
                .putString(EMAIL_ID_KEY, emailId)
                .putString(BLOB_ID_KEY, blobId)
                .build();
    }

    public static String uniqueName() {
        return "blob-download";
    }

    @NonNull
    @Override
    public Result doWork() {
        final Downloadable downloadable;
        try {
            downloadable = this.downloadable.get();
        } catch (final ExecutionException | InterruptedException e) {
            return Result.failure();
        }
        final EmailWithEncryptionStatus encryptionStatus =
                getDatabase().threadAndEmailDao().getEmailWithEncryptionStatus(emailId);
        if (encryptionStatus == null) {
            LOGGER.error("Unable to download blob {}. E-mail {} does not exist", blobId, emailId);
            return Result.failure();
        }
        // begin to display notification even if we don’t run as ForegroundService on Android 12
        updateProgress(downloadable, 0, true);
        if (encryptionStatus.getEncryptionStatus() == EncryptionStatus.PLAINTEXT) {
            return downloadEncryptedBlob(downloadable, encryptionStatus.encryptedBlobId);
        } else {
            return downloadCleartextBlob(downloadable);
        }
    }

    private Result downloadCleartextBlob(final Downloadable downloadable) {
        final BlobStorage storage = BlobStorage.get(getApplicationContext(), account, blobId);
        final File temporaryFile = storage.temporaryFile;
        final Mua mua = getMua();
        final long rangeStart = temporaryFile.exists() ? temporaryFile.length() : 0;
        final Long expectedSize = downloadable.getSize();
        final ListenableFuture<Download> downloadFuture = mua.download(downloadable, rangeStart);
        this.cancelableFuture = mua.download(downloadable, rangeStart);
        final Download download;
        try {
            download = downloadFuture.get();
        } catch (final ExecutionException e) {
            // TODO get cause. When cause FailureToResume delete tmp file
            final Throwable cause = e.getCause();
            LOGGER.warn("Unable to execute download request", cause);
            return Result.failure(Failure.of(cause));
        } catch (final InterruptedException e) {
            return Result.retry();
        }
        this.call = download.getCall();
        if (expectedSize != null && expectedSize != download.getContentLength()) {
            LOGGER.info(
                    "expected size {} does not match ContentLength {}",
                    expectedSize,
                    download.getContentLength());
        }
        try (final InputStream inputStream = download.getInputStream();
                final OutputStream outputStream =
                        new FileOutputStream(temporaryFile, download.isResumed())) {
            long transmitted = rangeStart;
            int count;
            final byte[] buffer = new byte[8192];
            while ((count = inputStream.read(buffer)) != -1) {
                if (isStopped() || this.call.isCanceled()) {
                    break;
                }
                transmitted += count;
                outputStream.write(buffer, 0, count);
                if (download.indeterminate() && expectedSize != null) {
                    updateProgress(
                            downloadable, Progress.progress(transmitted, expectedSize), false);
                } else {
                    updateProgress(
                            downloadable, download.progress(transmitted), download.indeterminate());
                }
            }
            outputStream.flush();
            // There seems to be a minimum display time of sorts. Even for very short running jobs
            // WorkManager will display the foreground notification for at least some time x.
            // However if the download finishes earlier the notification would still show
            // 'downloading'
            // stuck at 100% (Even though we already have the file and performed a view action)
            // Therefore we change the notification to 'Download complete' for the remainder of time
            // x.
            // For long running download jobs this will effectively not be shown
            notifyDownloadComplete(downloadable);
            LOGGER.info("Finished downloading {}", storage.temporaryFile.getAbsolutePath());
        } catch (final IOException e) {
            // TODO check if we want to retry
            LOGGER.warn("Unable to download file", e);
            return Result.failure(Failure.of(e));
        } finally {
            notificationManager.cancel(AttachmentNotification.DOWNLOAD_ID);
        }

        if (storage.moveTemporaryToFile()) {
            return getResult(downloadable, storage);
        } else {
            return Result.failure();
        }
    }

    private Result downloadEncryptedBlob(
            final Downloadable downloadable, final String encryptedBlobId) {
        final Downloadable encryptedBlob = EncryptedBodyPart.getDownloadable(encryptedBlobId);
        final AutocryptPlugin autocryptPlugin = getMua().getPlugin(AutocryptPlugin.class);
        final Map<String, BlobStorage> blobIdStorageMap = new HashMap<>();
        final ListenableFuture<Email> plaintextEmailFuture =
                autocryptPlugin.downloadAndDecrypt(
                        encryptedBlob,
                        (attachment, inputStream) -> {
                            final BlobStorage blobStorage =
                                    BlobStorage.get(
                                            getApplicationContext(),
                                            account,
                                            attachment.getBlobId());
                            final long bytesWritten;
                            try (final FileOutputStream fileOutputStream =
                                    new FileOutputStream(blobStorage.temporaryFile)) {
                                bytesWritten = ByteStreams.copy(inputStream, fileOutputStream);
                                if (blobStorage.moveTemporaryToFile()) {
                                    LOGGER.info(
                                            "Stored plaintext attachment {} to {} ({} bytes"
                                                    + " written)",
                                            attachment.getName(),
                                            blobStorage.getFile().getAbsolutePath(),
                                            bytesWritten);
                                } else {
                                    throw new IOException(
                                            "Unable to move attachment to non-temporary position");
                                }
                            }
                            blobIdStorageMap.put(attachment.getBlobId(), blobStorage);
                            return bytesWritten;
                        });
        this.cancelableFuture = plaintextEmailFuture;
        try {
            final Email plaintextEmail = plaintextEmailFuture.get();
            LOGGER.info(
                    "Cached {} plaintext attachments. Expected {}",
                    blobIdStorageMap.size(),
                    plaintextEmail.getAttachments().size());
            final BlobStorage storage = blobIdStorageMap.get(this.blobId);
            if (storage == null) {
                LOGGER.error(
                        "{} was not among downloaded blobs {}",
                        this.blobId,
                        blobIdStorageMap.keySet());
                return Result.failure();
            }
            notifyDownloadComplete(downloadable);
            return getResult(downloadable, storage);
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            LOGGER.error("Could not decrypt email", cause);
            return Result.failure();
        } catch (final InterruptedException e) {
            return Result.retry();
        } finally {
            notificationManager.cancel(AttachmentNotification.DOWNLOAD_ID);
        }
    }

    private Result getResult(final Downloadable downloadable, final BlobStorage storage) {
        final Uri uri =
                BlobStorage.getFileProviderUri(
                        getApplicationContext(), storage.file, downloadable.getName());
        final Data data =
                new Data.Builder()
                        .putString(URI_KEY, uri.toString()) // to be picked up by view intent
                        .putString(
                                StoreAttachmentWorker.FILE_KEY,
                                storage.file.getAbsolutePath()) // to be picked up by
                        // StoreAttachmentWorker
                        .build();
        return Result.success(data);
    }

    @Override
    public void onStopped() {
        super.onStopped();
        if (this.cancelableFuture != null) {
            if (this.cancelableFuture.cancel(true)) {
                LOGGER.info("Cancelled download future");
            }
        }
        if (this.call != null) {
            this.call.cancel();
        }
    }

    @NonNull
    @Override
    public ListenableFuture<ForegroundInfo> getForegroundInfoAsync() {
        return Futures.transform(
                downloadable,
                downloadable -> {
                    final Notification notification;
                    if (downloadable == null) {
                        notification =
                                AttachmentNotification.emailNotCached(getApplicationContext());
                    } else {
                        notification =
                                AttachmentNotification.downloading(
                                        getApplicationContext(), getId(), downloadable, 0, true);
                    }
                    return new ForegroundInfo(AttachmentNotification.DOWNLOAD_ID, notification);
                },
                MoreExecutors.directExecutor());
    }

    private void notifyDownloadComplete(final Downloadable downloadable) {
        getDatabase()
                .threadAndEmailDao()
                .incrementEmailBodyPartDownloadCount(this.emailId, downloadable.getBlobId());
        notificationManager.notify(
                AttachmentNotification.DOWNLOAD_ID,
                AttachmentNotification.downloaded(getApplicationContext(), downloadable));
    }

    private void updateProgress(
            final Downloadable downloadable, final int progress, final boolean indeterminate) {
        if (currentlyShownProgress == progress) {
            return;
        }
        if (notificationRateLimiter.tryAcquire()) {
            notificationManager.notify(
                    AttachmentNotification.DOWNLOAD_ID,
                    AttachmentNotification.downloading(
                            getApplicationContext(),
                            getId(),
                            downloadable,
                            progress,
                            indeterminate));
            this.currentlyShownProgress = progress;
        }
    }
}
