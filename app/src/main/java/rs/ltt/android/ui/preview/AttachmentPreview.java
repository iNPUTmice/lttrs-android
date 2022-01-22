package rs.ltt.android.ui.preview;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import rs.ltt.android.cache.BlobStorage;
import rs.ltt.android.cache.CachedAttachment;
import rs.ltt.android.cache.LocalAttachment;
import rs.ltt.android.entity.EmailBodyPartEntity;
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
        final File file = cachedAttachment.getFile();
        final MediaType mediaType = getMediaType();
        final Size size = getSize();
        final Bitmap preview;
        if (mediaType.is(MediaType.ANY_IMAGE_TYPE)) {
            preview = ImagePreview.getImagePreview(file, mediaType, size);
        } else if (mediaType.is(MediaType.ANY_VIDEO_TYPE)) {
            preview = VideoPreview.getVideoPreview(file, size);
        } else if (mediaType.is(MediaType.PDF)) {
            preview = PdfDocumentPreview.getPdfDocumentPreview(file, size);
        } else {
            throw new IllegalStateException();
        }
        final String cacheKey = key(accountId, attachment);
        BITMAP_CACHE.put(cacheKey, preview);
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
        if (shouldAttemptPreviewGeneration(mediaType)) {
            return getCachedAttachment(context);
        } else {
            LOGGER.debug("Could not generate preview for {}}", mediaType.toString());
            return Futures.immediateFuture(null);
        }
    }

    private static boolean shouldAttemptPreviewGeneration(final MediaType mediaType) {
        return mediaType.is(MediaType.ANY_IMAGE_TYPE)
                || mediaType.is(MediaType.ANY_VIDEO_TYPE)
                || mediaType.is(MediaType.PDF);
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
}
