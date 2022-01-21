package rs.ltt.android.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.net.MediaType;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.R;
import rs.ltt.android.entity.EmailBodyPartEntity;
import rs.ltt.android.ui.PreviewMeasurements;
import rs.ltt.android.util.MainThreadExecutor;
import rs.ltt.jmap.common.entity.Attachment;

public class AttachmentPreview {

    private static final Executor PREVIEW_EXECUTOR = Executors.newSingleThreadExecutor();

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachmentPreview.class);

    private final WeakReference<ImageView> imageViewWeakReference;
    final MediaType mediaType;
    final ListenableFuture<CachedAttachment> cachedAttachmentFuture;

    private AttachmentPreview(
            final ImageView imageView,
            final MediaType mediaType,
            final ListenableFuture<CachedAttachment> attachment) {
        this.imageViewWeakReference = new WeakReference<>(imageView);
        this.mediaType = mediaType;
        this.cachedAttachmentFuture = attachment;
    }

    public void load() {
        final ListenableFuture<Bitmap> previewFuture =
                Futures.transform(cachedAttachmentFuture, this::getPreview, PREVIEW_EXECUTOR);
        Futures.addCallback(
                previewFuture,
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(final Bitmap bitmap) {
                        final ImageView imageView = imageViewWeakReference.get();
                        if (imageView == null) {
                            return;
                        }
                        imageView.setVisibility(View.VISIBLE);
                        imageView.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable throwable) {
                        LOGGER.info("Attachment did not load", throwable);
                        final ImageView imageView = imageViewWeakReference.get();
                        if (imageView == null) {
                            return;
                        }
                        imageView.setVisibility(View.GONE);
                    }
                },
                MainThreadExecutor.getInstance());
    }

    private Bitmap getPreview(final CachedAttachment cachedAttachment) {
        if (mediaType.is(MediaType.ANY_IMAGE_TYPE)) {
            return getImagePreview(cachedAttachment.getFile());
        }
        throw new IllegalStateException();
    }

    private Bitmap getImagePreview(final File file) {
        final Size previewSize = getSize();
        final BitmapFactory.Options calculationOptions = new BitmapFactory.Options();
        calculationOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), calculationOptions);
        final int imageWidth = calculationOptions.outWidth;
        final int imageHeight = calculationOptions.outHeight;
        final PreviewMeasurements previewMeasurements =
                PreviewMeasurements.of(
                        imageWidth, imageHeight, previewSize.width, previewSize.height);
        LOGGER.info("original image size " + imageWidth + "x" + imageHeight);
        LOGGER.info("preview size: " + previewSize);
        LOGGER.debug("Using preview measurements {}", previewMeasurements);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = previewMeasurements.sampleSize;
        final Bitmap original = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        final Bitmap preview =
                Bitmap.createBitmap(
                        original,
                        previewMeasurements.x,
                        previewMeasurements.y,
                        previewMeasurements.width,
                        previewMeasurements.height);
        if (original != preview) {
            original.recycle();
        }
        // TODO scale down
        return preview;
    }

    private Size getSize() {
        final ImageView imageView = this.imageViewWeakReference.get();
        if (imageView == null) {
            throw new IllegalStateException("ImageView has been garbage collected");
        }
        final Context context = imageView.getContext();
        return new Size(
                context.getResources().getDimensionPixelSize(R.dimen.attachment_width),
                context.getResources().getDimensionPixelSize(R.dimen.attachment_preview_height));
    }

    public static AttachmentPreview of(
            @NonNull final ImageView imageView,
            @Nullable final Long accountId,
            @NonNull final Attachment attachment) {
        final MediaType mediaType = attachment.getMediaType();
        final ListenableFuture<CachedAttachment> cachedAttachmentFuture;
        if (mediaType == null) {
            cachedAttachmentFuture =
                    Futures.immediateFailedFuture(
                            new IllegalArgumentException(
                                    "Could not generate preview for unknown content type"));
        } else if (mediaType.is(MediaType.ANY_IMAGE_TYPE)) {
            if (attachment instanceof EmailBodyPartEntity) {
                cachedAttachmentFuture =
                        BlobStorage.getIfCached(imageView.getContext(), accountId, attachment);
            } else if (attachment instanceof LocalAttachment) {
                final LocalAttachment localAttachment = (LocalAttachment) attachment;
                cachedAttachmentFuture =
                        Futures.immediateFuture(
                                localAttachment.asCachedAttachment(imageView.getContext()));
            } else {
                throw new IllegalStateException(
                        String.format(
                                "Could not generate preview from %s",
                                attachment.getClass().getName()));
            }
        } else {
            cachedAttachmentFuture =
                    Futures.immediateFailedFuture(
                            new IllegalArgumentException(
                                    String.format(
                                            "Could not generate preview for %s",
                                            mediaType.toString())));
        }
        return new AttachmentPreview(imageView, mediaType, cachedAttachmentFuture);
    }

    private static class Size {
        private final int width;
        private final int height;

        private Size(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("width", width)
                    .add("height", height)
                    .toString();
        }
    }
}
