package rs.ltt.android.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.time.Instant;
import rs.ltt.android.entity.AccountStateEntity;
import rs.ltt.android.entity.PeerStateEntity;
import rs.ltt.autocrypt.client.header.EncryptionPreference;

@Dao
public abstract class AutocryptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(final AccountStateEntity accountState);

    @Query("select enabled from autocrypt_account where userId=:userId")
    public abstract LiveData<Boolean> isAutocryptEnabled(final String userId);

    @Query("select encryptionPreference from autocrypt_account where userId=:userId")
    public abstract LiveData<EncryptionPreference> getEncryptionPreference(final String userId);

    @Query("select * from autocrypt_account where userId=:userId")
    public abstract AccountStateEntity getAccountState(final String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(final PeerStateEntity peerState);

    @Transaction
    public boolean updateLastSeen(final String address, final Instant effectiveDate) {
        final LastSeenAutocryptTimestamp currentState = getLastSeenAutocryptTimestamp(address);
        if (currentState != null
                && currentState.autocryptTimestamp != null
                && effectiveDate.isBefore(currentState.autocryptTimestamp)) {
            return false;
        }
        if (currentState == null) {
            insert(PeerStateEntity.fresh(address, effectiveDate));
        } else if (currentState.lastSeen == null || effectiveDate.isAfter(currentState.lastSeen)) {
            if (updatePeerStateLastSeen(address, effectiveDate) != 1) {
                throw new IllegalStateException(
                        "Unable to autocrypt_peer.lastSeen. Peer does not exist.");
            }
        }
        return true;
    }

    @Query("update autocrypt_peer set lastSeen=:effectiveDate where address=:address")
    protected abstract int updatePeerStateLastSeen(
            final String address, final Instant effectiveDate);

    @Query("select address,lastSeen,autocryptTimestamp from autocrypt_peer where address=:address")
    protected abstract LastSeenAutocryptTimestamp getLastSeenAutocryptTimestamp(
            final String address);

    @Query("select * from autocrypt_peer where address=:address")
    public abstract PeerStateEntity getPeerState(final String address);

    @Query(
            "update autocrypt_peer set autocryptTimestamp=:effectiveDate, publicKey=:publicKey,"
                    + " encryptionPreference=:preference where address=:address")
    protected abstract int updatePeerState(
            final String address,
            final Instant effectiveDate,
            byte[] publicKey,
            final EncryptionPreference preference);

    @Transaction
    public void updateAutocrypt(
            final String address,
            final Instant effectiveDate,
            byte[] publicKey,
            final EncryptionPreference preference) {
        if (updatePeerState(address, effectiveDate, publicKey, preference) != 1) {
            throw new IllegalStateException("Unable to update autocrypt_peer. Peer does not exist");
        }
    }

    @Query("select address,gossipTimestamp from autocrypt_peer where address=:address")
    protected abstract GossipTimestamp getGossipTimestamp(final String address);

    @Query(
            "update autocrypt_peer set gossipTimestamp=:effectiveDate, gossipKey=:publicKey where"
                    + " address=:address")
    protected abstract int updateGossipKey(
            final String address, final Instant effectiveDate, final byte[] publicKey);

    @Transaction
    public boolean updateGossip(
            final String address, final Instant effectiveDate, final byte[] publicKey) {
        final GossipTimestamp gossipTimestamp = getGossipTimestamp(address);
        if (gossipTimestamp != null
                && gossipTimestamp.gossipTimestamp != null
                && effectiveDate.isBefore(gossipTimestamp.gossipTimestamp)) {
            return false;
        }
        if (gossipTimestamp == null) {
            insert(PeerStateEntity.freshGossip(address, effectiveDate, publicKey));
        } else {
            if (updateGossipKey(address, effectiveDate, publicKey) != 1) {
                throw new IllegalStateException(
                        "Unable to update autocrypt_peer. Peer does not exist");
            }
        }
        return true;
    }

    public static class LastSeenAutocryptTimestamp {
        public String address;
        public Instant lastSeen;
        public Instant autocryptTimestamp;
    }

    public static class GossipTimestamp {
        public String address;
        public Instant gossipTimestamp;
    }
}
