package rs.ltt.android.cache;

import java.time.Instant;
import rs.ltt.android.database.LttrsDatabase;
import rs.ltt.android.entity.AccountStateEntity;
import rs.ltt.autocrypt.client.header.EncryptionPreference;
import rs.ltt.autocrypt.client.storage.AccountState;
import rs.ltt.autocrypt.client.storage.PeerState;
import rs.ltt.autocrypt.client.storage.Storage;

public class AutocryptDatabaseStorage implements Storage {

    private final LttrsDatabase database;

    public AutocryptDatabaseStorage(final LttrsDatabase lttrsDatabase) {
        this.database = lttrsDatabase;
    }

    /**
     * Steps 1-2 of the update process 1) If the message’s effective date is older than the
     * peers[from-addr].autocrypt_timestamp value, then no changes are required, and the update
     * process terminates (returns false). 2) If the message’s effective date is more recent than
     * peers[from-addr].last_seen then set peers[from-addr].last_seen to the message’s effective
     * date.
     *
     * @param address The peer’s from address
     * @param effectiveDate The effective date of the message (sending time or the time of receipt
     *     if that date is in the future)
     * @return true if the effective data was more recent than the current autocrypt_timestamp
     */
    @Override
    public boolean updateLastSeen(final String address, final Instant effectiveDate) {
        return this.database.autocryptDao().updateLastSeen(address, effectiveDate);
    }

    /**
     * Steps 4-6 of the update process. 4) Set peers[from-addr].autocrypt_timestamp to the message’s
     * effective date. 5) Set peers[from-addr].public_key to the corresponding keydata value of the
     * Autocrypt header. 6) Set peers[from-addr].prefer_encrypt to the corresponding prefer-encrypt
     * value of the Autocrypt header.
     *
     * @param address The peer’s from address
     * @param effectiveDate The effective date of the message (sending time or the time of receipt
     *     if that date is in the future)
     * @param publicKey The key-data from the Autocrypt header
     * @param preference The prefer-encrypt value of the Autocrypt header
     */
    @Override
    public void updateAutocrypt(
            final String address,
            final Instant effectiveDate,
            byte[] publicKey,
            final EncryptionPreference preference) {
        this.database.autocryptDao().updateAutocrypt(address, effectiveDate, publicKey, preference);
    }

    @Override
    public boolean updateGossip(
            final String address, final Instant effectiveDate, final byte[] publicKey) {
        return this.database.autocryptDao().updateGossip(address, effectiveDate, publicKey);
    }

    @Override
    public PeerState getPeerState(final String address) {
        return this.database.autocryptDao().getPeerState(address);
    }

    @Override
    public AccountState getAccountState(final String userId) {
        return this.database.autocryptDao().getAccountState(userId);
    }

    @Override
    public void setAccountState(final String userId, final AccountState accountState) {
        this.database.autocryptDao().insert(AccountStateEntity.of(userId, accountState));
    }
}
