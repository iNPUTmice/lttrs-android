package rs.ltt.android.cache;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.google.common.base.CharMatcher;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rs.ltt.jmap.common.entity.Attachment;


public class BlobStorage {

    private static final Executor IO_EXECUTOR = Executors.newSingleThreadExecutor();

    private static final Logger LOGGER = LoggerFactory.getLogger(BlobStorage.class);

    public final File temporaryFile;
    public final File file;

    private BlobStorage(File temporaryFile, File file) {
        this.temporaryFile = temporaryFile;
        this.file = file;
    }

    public static BlobStorage get(final Context context, final long accountId, final String blobId) {
        final File blobDirectory = new File(context.getCacheDir(), "blobs");
        final File accountDirectory = new File(blobDirectory, String.format(Locale.US, "account-%d", accountId));
        if (accountDirectory.mkdirs()) {
            LOGGER.info("Created account directory {}", accountDirectory.getAbsolutePath());
        }
        final String cleanBlobId = CharMatcher.is(File.pathSeparatorChar).removeFrom(blobId);
        final File temporary = new File(accountDirectory, String.format(Locale.US, "%s.tmp", cleanBlobId));
        final File file = new File(accountDirectory, cleanBlobId);
        return new BlobStorage(temporary, file);
    }

    public static ListenableFuture<Uri> getFileProviderUri(final Context context, final long accountId, final Attachment attachment) {
        final ListenableFuture<BlobStorage> blobFuture = Futures.submit(() -> get(context, accountId, attachment.getBlobId()), IO_EXECUTOR);
        return Futures.transform(blobFuture, blobStorage -> {
            if (Objects.requireNonNull(blobStorage).file.exists()) {
                return getFileProviderUri(context, blobStorage.file, attachment.getName());
            } else {
                throw new InvalidCacheException(blobStorage.file);
            }
        }, IO_EXECUTOR);
    }

    public static ListenableFuture<BlobStorage> getIfCached(final Context context, final long accountId, final String blobId) {
        final ListenableFuture<BlobStorage> blobFuture = Futures.submit(() -> get(context, accountId, blobId), IO_EXECUTOR);
        return Futures.transform(blobFuture, blobStorage -> {
            if (Objects.requireNonNull(blobStorage).file.exists()) {
                return blobStorage;
            } else {
                throw new InvalidCacheException(blobStorage.file);
            }

        }, IO_EXECUTOR);
    }

    public static Uri getFileProviderUri(final Context context, File file, final String displayName) {
        final String authority = String.format("%s.provider.FileProvider", context.getPackageName());
        return FileProvider.getUriForFile(context, authority, file, displayName);
    }

    public boolean moveTemporaryToFile() {
        if (file.delete()) {
            LOGGER.warn("Deleted preexisting blob file {}", file.getAbsolutePath());
        }
        return temporaryFile.renameTo(file);
    }

    public static class InvalidCacheException extends IllegalStateException {

        InvalidCacheException(final File file) {
            super(String.format("%s not found", file.getAbsolutePath()));
        }
    }
}
