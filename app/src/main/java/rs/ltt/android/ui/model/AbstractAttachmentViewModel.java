package rs.ltt.android.ui.model;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.google.common.base.Preconditions;
import com.google.common.net.MediaType;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.R;
import rs.ltt.android.cache.BlobStorage;
import rs.ltt.android.ui.ViewIntent;
import rs.ltt.android.util.Event;
import rs.ltt.android.util.MainThreadExecutor;
import rs.ltt.android.worker.BlobDownloadWorker;
import rs.ltt.android.worker.Failure;
import rs.ltt.jmap.common.entity.Attachment;

public abstract class AbstractAttachmentViewModel extends AndroidViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAttachmentViewModel.class);

    private final MediatorLiveData<Event<ViewIntent>> viewIntentEvent = new MediatorLiveData<>();
    private final MutableLiveData<Event<String>> downloadFailure = new MutableLiveData<>();

    protected AbstractAttachmentViewModel(@NonNull @NotNull Application application) {
        super(application);
    }

    protected abstract long getAccountId();

    public void open(final String emailId, final Attachment attachment) {
        Futures.addCallback(
                getFileProviderUri(attachment),
                new FutureCallback<Uri>() {
                    @Override
                    public void onSuccess(@Nullable Uri uri) {
                        viewIntentEvent.postValue(
                                new Event<>(new ViewIntent(uri, attachment.getMediaType())));
                    }

                    @Override
                    public void onFailure(@NotNull Throwable throwable) {
                        if (throwable instanceof BlobStorage.InvalidCacheException) {
                            queueDownload(emailId, attachment);
                        } else {
                            // TODO show error message?
                        }
                    }
                },
                MainThreadExecutor.getInstance());
    }

    protected void queueDownload(final String emailId, final Attachment attachment) {
        Preconditions.checkNotNull(
                emailId, "Can not download attachment without specifying emailId");
        final WorkManager workManager = WorkManager.getInstance(getApplication());
        final OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(BlobDownloadWorker.class)
                        .setInputData(
                                BlobDownloadWorker.data(
                                        getAccountId(), emailId, attachment.getBlobId()))
                        .build();
        workManager.enqueueUniqueWork(
                BlobDownloadWorker.uniqueName(), ExistingWorkPolicy.APPEND_OR_REPLACE, workRequest);
        final LiveData<WorkInfo> workInfo =
                workManager.getWorkInfoByIdLiveData(workRequest.getId());
        waitForDownload(workInfo, attachment.getMediaType());
    }

    private void waitForDownload(
            final LiveData<WorkInfo> workInfoLiveData, final MediaType mediaType) {
        viewIntentEvent.addSource(
                workInfoLiveData,
                workInfo -> {
                    if (workInfo.getState().isFinished()) {
                        viewIntentEvent.removeSource(workInfoLiveData);
                        processFinishedDownload(workInfo, mediaType);
                    }
                });
    }

    private void processFinishedDownload(final WorkInfo workInfo, final MediaType mediaType) {
        final WorkInfo.State state = workInfo.getState();
        if (state == WorkInfo.State.SUCCEEDED) {
            final Uri uri = BlobDownloadWorker.getUri(workInfo);
            viewIntentEvent.postValue(new Event<>(new ViewIntent(uri, mediaType)));
        } else if (state == WorkInfo.State.FAILED) {
            final Failure failure;
            try {
                failure = Failure.of(workInfo.getOutputData());
            } catch (final IllegalArgumentException e) {
                LOGGER.warn("Unable to extract failure from failed worker", e);
                return;
            }
            if (failure instanceof Failure.BlobTransferFailure) {
                final Failure.BlobTransferFailure blobTransferFailure =
                        (Failure.BlobTransferFailure) failure;
                final String message =
                        getApplication()
                                .getString(
                                        R.string.attachment_download_failed_status_code_x,
                                        blobTransferFailure.getStatusCode());
                downloadFailure.postValue(new Event<>(message));
            } else {
                downloadFailure.postValue(new Event<>(failure.getMessage()));
            }
        }
    }

    public LiveData<Event<ViewIntent>> getViewIntentEvent() {
        return this.viewIntentEvent;
    }

    public LiveData<Event<String>> getDownloadErrorEvent() {
        return this.downloadFailure;
    }

    private ListenableFuture<Uri> getFileProviderUri(final Attachment attachment) {
        return BlobStorage.getFileProviderUri(getApplication(), getAccountId(), attachment);
    }
}
