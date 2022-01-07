package rs.ltt.android.entity;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

import rs.ltt.jmap.common.entity.Identifiable;

public class EmailWithEncryptionStatus implements Identifiable {

    public String id;

    public EncryptionStatus encryptionStatus;

    public String encryptedBlobId;

    public EncryptionStatus getEncryptionStatus() {
        return encryptionStatus == null ? EncryptionStatus.CLEARTEXT : encryptionStatus;
    }

    @Override
    public String getId() {
        return this.id;
    }

    public boolean isCleartext() {
        return Strings.isNullOrEmpty(encryptedBlobId)
                || getEncryptionStatus() == EncryptionStatus.CLEARTEXT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailWithEncryptionStatus that = (EmailWithEncryptionStatus) o;
        return Objects.equal(id, that.id)
                && encryptionStatus == that.encryptionStatus
                && Objects.equal(encryptedBlobId, that.encryptedBlobId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, encryptionStatus, encryptedBlobId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("encryptionStatus", encryptionStatus)
                .add("encryptedBlobId", encryptedBlobId)
                .toString();
    }
}
