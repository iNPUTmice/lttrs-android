package rs.ltt.android.ui.model;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
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

import rs.ltt.android.cache.BlobStorage;
import rs.ltt.android.ui.ViewIntent;
import rs.ltt.android.util.Event;
import rs.ltt.android.util.MainThreadExecutor;
import rs.ltt.android.worker.BlobDownloadWorker;
import rs.ltt.jmap.common.entity.Attachment;

public abstract class AbstractAttachmentViewModel extends AndroidViewModel {

    private final MediatorLiveData<Event<ViewIntent>> viewIntentEvent = new MediatorLiveData<>();

    protected AbstractAttachmentViewModel(@NonNull @NotNull Application application) {
        super(application);
    }

    protected abstract long getAccountId();

    public void open(final String emailId, final Attachment attachment) {
        Futures.addCallback(getFileProviderUri(attachment.getBlobId()), new FutureCallback<Uri>() {
            @Override
            public void onSuccess(@Nullable Uri uri) {
                viewIntentEvent.postValue(new Event<>(new ViewIntent(uri, attachment.getMediaType())));
            }

            @Override
            public void onFailure(@NotNull Throwable throwable) {
                if (throwable instanceof BlobStorage.InvalidCacheException) {
                    queueDownload(emailId, attachment);
                } else {
                    //TODO show error message?
                }
            }
        }, MainThreadExecutor.getInstance());
    }

    protected void queueDownload(final String emailId, final Attachment attachment) {
        Preconditions.checkNotNull(emailId, "Can not download attachment without specifying emailId");
        final WorkManager workManager = WorkManager.getInstance(getApplication());
        final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(BlobDownloadWorker.class)
                .setInputData(BlobDownloadWorker.data(getAccountId(), emailId, attachment.getBlobId()))
                .build();
        workManager.enqueueUniqueWork(
                BlobDownloadWorker.uniqueName(),
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                workRequest
        );
        final LiveData<WorkInfo> workInfo = workManager.getWorkInfoByIdLiveData(workRequest.getId());
        waitForDownload(attachment.getMediaType(), workInfo);
    }

    private void waitForDownload(final MediaType mediaType, final LiveData<WorkInfo> workInfoLiveData) {
        viewIntentEvent.addSource(workInfoLiveData, workInfo -> {
            final WorkInfo.State state = workInfo.getState();
            if (state.isFinished()) {
                viewIntentEvent.removeSource(workInfoLiveData);
                if (state == WorkInfo.State.SUCCEEDED) {
                    final Uri uri = BlobDownloadWorker.getUri(workInfo);
                    viewIntentEvent.postValue(new Event<>(new ViewIntent(uri, mediaType)));
                }
                //TODO show some form of error
            }
        });
    }

    public LiveData<Event<ViewIntent>> getViewIntentEvent() {
        return this.viewIntentEvent;
    }

    private ListenableFuture<Uri> getFileProviderUri(final String blobId) {
        return BlobStorage.getFileProviderUri(getApplication(), getAccountId(), blobId);
    }

}
