package rs.ltt.android.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.common.base.MoreObjects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Weigher;
import com.google.common.net.MediaType;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Locale;
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

    private static final Cache<String, Bitmap> BITMAP_CACHE =
            CacheBuilder.newBuilder()
                    .weigher((Weigher<String, Bitmap>) (s, bitmap) -> bitmap.getByteCount())
                    .maximumWeight(10_000_000)
                    .build();

    private final WeakReference<ImageView> imageViewWeakReference;
    private final Long accountId;
    private final Attachment attachment;

    private AttachmentPreview(
            @NonNull final ImageView imageView,
            @Nullable final Long accountId,
            @NonNull final Attachment attachment) {
        this.imageViewWeakReference = new WeakReference<>(imageView);
        this.accountId = accountId;
        this.attachment = attachment;
    }

    public void load() {
        final ListenableFuture<Bitmap> previewFuture;
        final Bitmap preview = BITMAP_CACHE.getIfPresent(key(accountId, attachment));
        if (preview != null) {
            previewFuture = Futures.immediateFuture(preview);
        } else {
            previewFuture =
                    Futures.transform(getCachedAttachment(), this::getPreview, PREVIEW_EXECUTOR);
        }
        Futures.addCallback(
                previewFuture,
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(final Bitmap bitmap) {
                        final ImageView imageView = imageViewWeakReference.get();
                        if (imageView == null) {
                            return;
                        }
                        setImageBitmap(imageView, bitmap);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable throwable) {
                        LOGGER.info("Attachment did not load", throwable);
                        final ImageView imageView = imageViewWeakReference.get();
                        if (imageView == null) {
                            return;
                        }
                        setImageBitmap(imageView, null);
                    }
                },
                MainThreadExecutor.getInstance());
    }

    private static void setImageBitmap(final ImageView imageView, @Nullable final Bitmap preview) {
        if (preview == null) {
            imageView.setVisibility(View.GONE);
        } else {
            imageView.setImageBitmap(preview);
            imageView.setVisibility(View.VISIBLE);
        }
    }

    private static String key(final Long accountId, final Attachment attachment) {
        if (attachment instanceof LocalAttachment) {
            return ((LocalAttachment) attachment).getUuid().toString();
        } else if (accountId == null) {
            return attachment.getBlobId();
        } else {
            return String.format(Locale.US, "%d-%s", accountId, attachment.getBlobId());
        }
    }

    private @Nullable Bitmap getPreview(@Nullable final CachedAttachment cachedAttachment) {
        if (cachedAttachment == null) {
            return null;
        }
        final MediaType mediaType = getMediaType();
        final Bitmap preview;
        if (mediaType.is(MediaType.ANY_IMAGE_TYPE)) {
            preview = getImagePreview(cachedAttachment.getFile());
        } else {
            throw new IllegalStateException();
        }
        final String cacheKey = key(accountId, attachment);
        BITMAP_CACHE.put(cacheKey, preview);
        return preview;
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
        LOGGER.debug("original image size " + imageWidth + "x" + imageHeight);
        LOGGER.debug("preview size: " + previewSize);
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
        LOGGER.info("Preview size {} bytes", preview.getByteCount());
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
        return new AttachmentPreview(imageView, accountId, attachment);
    }

    private ListenableFuture<CachedAttachment> getCachedAttachment() {
        final ImageView imageView = this.imageViewWeakReference.get();
        if (imageView == null) {
            return Futures.immediateFailedFuture(
                    new IllegalStateException("ImageView has been garbage collected"));
        }
        final Context context = imageView.getContext();
        final MediaType mediaType = getMediaType();
        if (mediaType.is(MediaType.ANY_IMAGE_TYPE)) {
            return getCachedAttachment(context);
        } else {
            LOGGER.debug("Could not generate preview for {}}", mediaType.toString());
            return Futures.immediateFuture(null);
        }
    }

    private ListenableFuture<CachedAttachment> getCachedAttachment(final Context context) {
        if (attachment instanceof EmailBodyPartEntity) {
            if (accountId == null) {
                return Futures.immediateFailedFuture(
                        new IllegalArgumentException("Could not generate preview with accountId"));
            } else {
                return BlobStorage.getIfCached(context, accountId, attachment);
            }
        } else if (attachment instanceof LocalAttachment) {
            final LocalAttachment localAttachment = (LocalAttachment) attachment;
            return Futures.immediateFuture(localAttachment.asCachedAttachment(context));
        } else {
            throw new IllegalStateException(
                    String.format(
                            "Could not generate preview from %s", attachment.getClass().getName()));
        }
    }

    private @NonNull MediaType getMediaType() {
        final MediaType mediaType = attachment.getMediaType();
        return mediaType == null ? MediaType.OCTET_STREAM : mediaType;
    }

    private static class Size {
        private final int width;
        private final int height;

        private Size(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @NonNull
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("width", width)
                    .add("height", height)
                    .toString();
        }
    }
}
