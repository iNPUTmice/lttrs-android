package rs.ltt.android.worker;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkInfo;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import java.util.Collection;
import rs.ltt.jmap.client.blob.BlobTransferException;
import rs.ltt.jmap.client.blob.MaxUploadSizeExceededException;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRoleAndName;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.mua.service.exception.PreexistingMailboxException;

public class Failure {

    private static final String EXCEPTION = "exception";
    private static final String MESSAGE = "message";
    private static final String PREEXISTING_MAILBOX_ID = "preexisting_mailbox_id";
    private static final String TARGET_ROLE = "role";
    private static final String MAX_UPLOAD_SIZE = "max_upload_size";
    private static final String HTTP_STATUS_CODE = "status_code";

    private final Class<?> exception;
    private final String message;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Failure failure = (Failure) o;
        return Objects.equal(exception, failure.exception)
                && Objects.equal(message, failure.message);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(exception, message);
    }

    private Failure(final Class<?> exception, final String message) {
        this.exception = exception;
        this.message = message;
    }

    public static Failure of(final Data data) {
        final String exception = data.getString(EXCEPTION);
        final Class<?> clazz;
        try {
            clazz = Class.forName(Strings.nullToEmpty(exception));
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
        if (clazz == PreexistingMailboxException.class) {
            return new PreExistingMailbox(
                    clazz,
                    data.getString(MESSAGE),
                    data.getString(PREEXISTING_MAILBOX_ID),
                    Role.valueOf(data.getString(TARGET_ROLE)));
        } else if (clazz == MaxUploadSizeExceededException.class) {
            return new MaxUploadSizeExceeded(
                    clazz, data.getString(MESSAGE), data.getLong(MAX_UPLOAD_SIZE, 0));
        } else if (clazz == BlobTransferException.class) {
            return new BlobTransferFailure(
                    clazz, data.getString(MESSAGE), data.getInt(HTTP_STATUS_CODE, 0));
        } else {
            return new Failure(clazz, data.getString(MESSAGE));
        }
    }

    public static Collection<Failure> of(final Collection<WorkInfo> workInfoList) {
        final Collection<Data> data = Collections2.transform(workInfoList, WorkInfo::getOutputData);
        return Collections2.transform(data, Failure::of);
    }

    static Data of(final Throwable cause) {
        final Data.Builder dataBuilder = new Data.Builder();
        if (cause == null) {
            return dataBuilder.build();
        }
        dataBuilder.putString(EXCEPTION, cause.getClass().getName());
        final String message = cause.getMessage();
        if (!Strings.isNullOrEmpty(message)) {
            dataBuilder.putString(MESSAGE, message);
        }
        if (cause instanceof PreexistingMailboxException) {
            final IdentifiableMailboxWithRoleAndName preexistingMailbox =
                    ((PreexistingMailboxException) cause).getPreexistingMailbox();
            final Role targetRole = ((PreexistingMailboxException) cause).getTargetRole();
            dataBuilder.putString(PREEXISTING_MAILBOX_ID, preexistingMailbox.getId());
            dataBuilder.putString(TARGET_ROLE, targetRole.toString());
        }
        if (cause instanceof MaxUploadSizeExceededException) {
            final MaxUploadSizeExceededException exception = (MaxUploadSizeExceededException) cause;
            dataBuilder.putLong(MAX_UPLOAD_SIZE, exception.getMaxFileSize());
        }
        if (cause instanceof BlobTransferException) {
            final BlobTransferException exception = (BlobTransferException) cause;
            dataBuilder.putInt(HTTP_STATUS_CODE, exception.getCode());
        }
        return dataBuilder.build();
    }

    @NonNull
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("exception", exception)
                .add("message", message)
                .toString();
    }

    public Class<?> getException() {
        return exception;
    }

    public String getMessage() {
        return message;
    }

    public static class PreExistingMailbox extends Failure {

        private final String mailboxId;
        private final Role role;

        private PreExistingMailbox(
                Class<?> exception, String message, final String mailboxId, final Role role) {
            super(exception, message);
            this.mailboxId = mailboxId;
            this.role = role;
        }

        public String getMailboxId() {
            return mailboxId;
        }

        public Role getRole() {
            return role;
        }
    }

    public static class MaxUploadSizeExceeded extends Failure {
        private final long maxUploadSize;

        private MaxUploadSizeExceeded(
                Class<?> exception, String message, final long maxUploadSize) {
            super(exception, message);
            this.maxUploadSize = maxUploadSize;
        }

        public long getMaxUploadSize() {
            return maxUploadSize;
        }
    }

    public static class BlobTransferFailure extends Failure {
        private final int statusCode;

        private BlobTransferFailure(Class<?> exception, String message, final int statusCode) {
            super(exception, message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return this.statusCode;
        }
    }
}
