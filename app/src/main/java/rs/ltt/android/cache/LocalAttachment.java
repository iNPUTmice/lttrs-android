package rs.ltt.android.cache;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.net.MediaType;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.common.entity.Attachment;

public class LocalAttachment implements Attachment {

    private static final String LOCAL_ATTACHMENT_DIRECTORY = "local-attachment";

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalAttachment.class);

    private final UUID uuid;
    private final MediaType mediaType;
    private final String name;
    private final long size;

    public LocalAttachment(final UUID uuid, MediaType mediaType, String name, long size) {
        this.uuid = uuid;
        this.mediaType = mediaType;
        this.name = name;
        this.size = size;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    @Override
    public String getCharset() {
        final Optional<Charset> charset = mediaType.charset();
        return charset.isPresent() ? charset.toString() : null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getBlobId() {
        throw new IllegalStateException(
                String.format(
                        "A blobId does not exist for %s", LocalAttachment.class.getSimpleName()));
    }

    @Override
    public String getType() {
        return String.format("%s/%s", this.mediaType.type(), this.mediaType.subtype());
    }

    @Override
    public Long getSize() {
        return this.size;
    }

    public static LocalAttachment of(final Context context, final Uri uri) {
        final ContentResolver contentResolver = context.getContentResolver();
        final String type = contentResolver.getType(uri);
        final long size;
        final String name;
        try (final Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
            cursor.moveToFirst();
            size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
            name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
        }
        return new LocalAttachment(UUID.randomUUID(), MediaType.parse(type), name, size);
    }

    public static void cache(
            final Context context, final Uri uri, final LocalAttachment localAttachment) {
        final ContentResolver contentResolver = context.getContentResolver();
        final File file = asFile(context, localAttachment);
        try (final InputStream inputStream = contentResolver.openInputStream(uri);
                final OutputStream outputStream = new FileOutputStream(file)) {
            final long total = copy(inputStream, outputStream);
            LOGGER.info(
                    "copied {} bytes to {}. Reported size was {}",
                    total,
                    file.getAbsolutePath(),
                    localAttachment.size);
        } catch (final IOException e) {
            if (file.delete()) {
                LOGGER.info("Clean up file {}", file.getAbsolutePath());
            }
            throw new CacheWriteException(e);
        }
    }

    public static File asFile(Context context, final LocalAttachment attachment) {
        final File directory = new File(context.getCacheDir(), LOCAL_ATTACHMENT_DIRECTORY);
        if (directory.mkdirs()) {
            LOGGER.info("Created local attachment directory {}", directory.getAbsolutePath());
        }
        return new File(directory, attachment.uuid.toString());
    }

    public static void delete(final Context context, final LocalAttachment attachment) {
        final File file = asFile(context, attachment);
        if (file.delete()) {
            LOGGER.info("Clean up unused file {}", file.getAbsolutePath());
        }
    }

    private static long copy(InputStream from, OutputStream to) throws IOException {
        Preconditions.checkNotNull(from);
        Preconditions.checkNotNull(to);
        final byte[] buffer = new byte[8096];
        long total = 0;
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedIOException();
            }
            final int read = from.read(buffer);
            if (read == -1) {
                break;
            }
            to.write(buffer, 0, read);
            total += read;
        }
        return total;
    }

    public static ListenableFuture<Uri> getFileProviderUri(
            final Context context, final LocalAttachment attachment) {
        return Futures.immediateFuture(
                BlobStorage.getFileProviderUri(
                        context, asFile(context, attachment), attachment.getName()));
    }

    public static class CacheWriteException extends RuntimeException {
        private CacheWriteException(final IOException e) {
            super("Could not cache local attachment", e);
        }
    }
}
