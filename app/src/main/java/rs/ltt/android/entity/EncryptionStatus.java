package rs.ltt.android.entity;

public enum EncryptionStatus {
    CLEARTEXT, // email was not encrypted at all
    ENCRYPTED, // email is currently encrypted with PGP
    PLAINTEXT, // email has been decrypted successfully
    FAILED // there has been an error decrypting the email
}
