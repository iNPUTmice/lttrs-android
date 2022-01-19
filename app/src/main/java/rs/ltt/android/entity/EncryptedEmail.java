package rs.ltt.android.entity;

import com.google.common.base.Strings;

public class EncryptedEmail extends EmailPreview {

    public String encryptedBlobId;

    public boolean isCleartext() {
        return Strings.isNullOrEmpty(encryptedBlobId)
                || getEncryptionStatus() == EncryptionStatus.CLEARTEXT;
    }
}
