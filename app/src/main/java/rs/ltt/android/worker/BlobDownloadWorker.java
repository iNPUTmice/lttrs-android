package rs.ltt.android.worker;

import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkInfo;
import androidx.work.WorkerParameters;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.RateLimiter;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

import okhttp3.Call;
import rs.ltt.android.cache.BlobStorage;
import rs.ltt.android.entity.DownloadableBlob;
import rs.ltt.android.ui.notification.AttachmentNotification;
import rs.ltt.jmap.client.blob.Download;
import rs.ltt.jmap.client.blob.Progress;
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
    private DownloadableBlob downloadable;
    private Call call;
    private ListenableFuture<Download> downloadFuture;
    private int currentlyShownProgress = 0;

    public BlobDownloadWorker(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = workerParams.getInputData();
        this.emailId = data.getString(EMAIL_ID_KEY);
        this.blobId = data.getString(BLOB_ID_KEY);
        this.notificationManager = context.getSystemService(NotificationManager.class);
    }

    public static Uri getUri(final WorkInfo workInfo) {
        Preconditions.checkState(workInfo.getState() == WorkInfo.State.SUCCEEDED, "Work must have succeeded to extract uri");
        final Data data = workInfo.getOutputData();
        final String uri = Preconditions.checkNotNull(data.getString(URI_KEY), "OutputData is missing URI");
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
    @NotNull
    @Override
    public Result doWork() {
        this.downloadable = getDatabase().threadAndEmailDao().getDownloadable(emailId, blobId);
        final BlobStorage storage = BlobStorage.get(getApplicationContext(), account, blobId);
        final File temporaryFile = storage.temporaryFile;
        final Mua mua = getMua();
        final long rangeStart = temporaryFile.exists() ? temporaryFile.length() : 0;
        final Long expectedSize = this.downloadable.getSize();
        setForegroundAsync(getForegroundInfo());
        this.downloadFuture = mua.download(downloadable, rangeStart);
        final Download download;
        try {
            download = this.downloadFuture.get();
        } catch (final ExecutionException | InterruptedException e) {
            LOGGER.warn("Unable to execute download request", e);
            return Result.failure();
        }
        this.call = download.getCall();
        if (expectedSize != null && expectedSize != download.getContentLength()) {
            LOGGER.info("expected size {} does not match ContentLength {}", expectedSize, download.getContentLength());
        }
        try (final InputStream inputStream = download.getInputStream();
             final OutputStream outputStream = new FileOutputStream(temporaryFile, download.isResumed())) {
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
                    updateProgress(Progress.progress(transmitted, expectedSize), false);
                } else {
                    updateProgress(download.progress(transmitted), download.indeterminate());
                }
            }
            outputStream.flush();
        } catch (final Exception e) {
            //TODO get cause. When cause FailureToResume delete tmp file
            LOGGER.warn("Unable to download file", e);
            return Result.failure();
        }

        //There seems to be a minimum display time of sorts. Even for very short running jobs
        //WorkManager will display the foreground notification for at least some time x.
        //However if the download finishes earlier the notification would still show 'downloading'
        //stuck at 100% (Even though we already have the file and performed a view action)
        //Therefore we change the notification to 'Download complete' for the remainder of time x.
        //For long running download jobs this will effectively not be shown
        notifyDownloadComplete();
        LOGGER.info("Finished downloading {}", storage.temporaryFile.getAbsolutePath());
        if (storage.moveTemporaryToFile()) {
            final Uri uri = BlobStorage.getFileProviderUri(
                    getApplicationContext(), storage.file, downloadable.getName()
            );
            final Data data = new Data.Builder()
                    .putString(URI_KEY, uri.toString()) //to be picked up by view intent
                    .putString(StoreAttachmentWorker.FILE_KEY, storage.file.getAbsolutePath()) //to be picked up by StoreAttachmentWorker
                    .build();
            return Result.success(data);
        } else {
            return Result.failure();
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        if (this.downloadFuture != null) {
            if (this.downloadFuture.cancel(true)) {
                LOGGER.info("Cancelled download future");
            }
        }
        if (this.call != null) {
            this.call.cancel();
        }
    }

    private ForegroundInfo getForegroundInfo() {
        final DownloadableBlob downloadable = Preconditions.checkNotNull(
                this.downloadable,
                "getForegroundInfo can only be called after setting downloadable"
        );
        return new ForegroundInfo(AttachmentNotification.DOWNLOAD_ID, AttachmentNotification.downloading(
                getApplicationContext(),
                getId(),
                downloadable,
                0,
                true
        ));
    }

    private void notifyDownloadComplete() {
        final DownloadableBlob downloadable = Preconditions.checkNotNull(
                this.downloadable,
                "getForegroundInfo can only be called after setting downloadable"
        );
        notificationManager.notify(AttachmentNotification.DOWNLOAD_ID, AttachmentNotification.downloaded(
                getApplicationContext(),
                downloadable
        ));
    }

    private void updateProgress(final int progress, final boolean indeterminate) {
        if (currentlyShownProgress == progress) {
            return;
        }
        if (notificationRateLimiter.tryAcquire()) {
            final DownloadableBlob downloadable = Preconditions.checkNotNull(
                    this.downloadable,
                    "getForegroundInfo can only be called after setting downloadable"
            );
            notificationManager.notify(AttachmentNotification.DOWNLOAD_ID, AttachmentNotification.downloading(
                    getApplicationContext(),
                    getId(),
                    downloadable,
                    progress,
                    indeterminate
            ));
            this.currentlyShownProgress = progress;
        }
    }
}
