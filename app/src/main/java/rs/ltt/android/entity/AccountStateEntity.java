package rs.ltt.android.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import rs.ltt.autocrypt.client.header.EncryptionPreference;
import rs.ltt.autocrypt.client.storage.AccountState;

@Entity(tableName = "autocrypt_account")
public class AccountStateEntity implements AccountState {

    @NonNull @PrimaryKey public String userId;

    public boolean enabled;

    public EncryptionPreference encryptionPreference;

    public byte[] secretKey;

    public AccountStateEntity() {}

    private AccountStateEntity(
            @NonNull final String userId,
            final boolean enabled,
            final EncryptionPreference encryptionPreference,
            final byte[] secretKey) {
        this.userId = userId;
        this.enabled = enabled;
        this.encryptionPreference = encryptionPreference;
        this.secretKey = secretKey;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public byte[] getSecretKey() {
        return this.secretKey;
    }

    @Override
    public EncryptionPreference getEncryptionPreference() {
        return encryptionPreference;
    }

    public static AccountStateEntity of(final String userId, final AccountState accountState) {
        return new AccountStateEntity(
                userId,
                accountState.isEnabled(),
                accountState.getEncryptionPreference(),
                accountState.getSecretKey());
    }
}
