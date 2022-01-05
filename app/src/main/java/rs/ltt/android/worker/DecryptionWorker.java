package rs.ltt.android.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.entity.EmailWithEncryptionStatus;
import rs.ltt.android.entity.EncryptionStatus;
import rs.ltt.autocrypt.jmap.AutocryptPlugin;
import rs.ltt.autocrypt.jmap.EncryptedBodyPart;
import rs.ltt.autocrypt.jmap.mime.AttachmentRetriever;
import rs.ltt.jmap.common.entity.Attachment;
import rs.ltt.jmap.common.entity.Downloadable;
import rs.ltt.jmap.common.entity.Email;

public class DecryptionWorker extends AbstractMuaWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecryptionWorker.class);

    private static final String EMAIL_ID = "emailId";

    private final String emailId;

    public DecryptionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = workerParams.getInputData();
        this.emailId = data.getString(EMAIL_ID);
    }

    @NonNull
    @Override
    public Result doWork() {
        final EmailWithEncryptionStatus originalEmail =
                getDatabase().threadAndEmailDao().getEmailWithEncryptionStatus(this.emailId);
        if (Strings.isNullOrEmpty(originalEmail.encryptedBlobId)
                || originalEmail.getEncryptionStatus() == EncryptionStatus.CLEARTEXT) {
            return Result.failure();
        }
        final Downloadable encryptedBlob =
                EncryptedBodyPart.getDownloadable(originalEmail.encryptedBlobId);
        final AutocryptPlugin autocryptPlugin = getMua().getPlugin(AutocryptPlugin.class);
        final ListenableFuture<Email> plaintextEmailFuture =
                autocryptPlugin.downloadAndDecrypt(
                        encryptedBlob,
                        new AttachmentRetriever() {
                            @Override
                            public void onAttachmentRetrieved(
                                    Attachment attachment, InputStream inputStream)
                                    throws IOException {}
                        });
        try {
            final Email plaintextEmail =
                    plaintextEmailFuture.get().toBuilder().id(originalEmail.getId()).build();
            getDatabase().threadAndEmailDao().setPlaintextBodyParts(plaintextEmail);
            return Result.success();
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            LOGGER.error("Could not decrypt email", cause);
            return Result.failure();
        } catch (final InterruptedException e) {
            return Result.retry();
        }
    }

    public static Data data(final Long account, final String emailId) {
        return new Data.Builder()
                .putLong(ACCOUNT_KEY, account)
                .putString(EMAIL_ID, emailId)
                .build();
    }

    public static String uniqueName(final Long accountId, final String emailId) {
        return String.format(Locale.ENGLISH, "decrypt-%d-%s", accountId, emailId);
    }
}
