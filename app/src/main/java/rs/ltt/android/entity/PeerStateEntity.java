package rs.ltt.android.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.time.Instant;
import rs.ltt.autocrypt.client.header.EncryptionPreference;
import rs.ltt.autocrypt.client.storage.PeerState;

@Entity(tableName = "autocrypt_peer")
public class PeerStateEntity implements PeerState {

    @NonNull @PrimaryKey public String address;
    public Instant lastSeen;
    public Instant autocryptTimestamp;
    public Instant gossipTimestamp;
    public byte[] publicKey;
    public byte[] gossipKey;
    public EncryptionPreference encryptionPreference;

    public static PeerStateEntity fresh(final String address, Instant effectiveDate) {
        final PeerStateEntity peerStateEntity = new PeerStateEntity();
        peerStateEntity.address = address;
        peerStateEntity.lastSeen = effectiveDate;
        peerStateEntity.autocryptTimestamp = Instant.EPOCH;
        peerStateEntity.gossipTimestamp = Instant.EPOCH;
        return peerStateEntity;
    }

    public static PeerStateEntity freshGossip(
            final String address, Instant effectiveDate, final byte[] gossipKey) {
        final PeerStateEntity peerStateEntity = new PeerStateEntity();
        peerStateEntity.address = address;
        peerStateEntity.lastSeen = Instant.EPOCH;
        peerStateEntity.autocryptTimestamp = Instant.EPOCH;
        peerStateEntity.gossipTimestamp = effectiveDate;
        peerStateEntity.gossipKey = gossipKey;
        return peerStateEntity;
    }

    @Override
    public Instant getLastSeen() {
        return this.lastSeen;
    }

    @Override
    public Instant getAutocryptTimestamp() {
        return this.autocryptTimestamp;
    }

    @Override
    public Instant getGossipTimestamp() {
        return this.gossipTimestamp;
    }

    @Override
    public byte[] getPublicKey() {
        return this.publicKey;
    }

    @Override
    public byte[] getGossipKey() {
        return this.gossipKey;
    }

    @Override
    public EncryptionPreference getEncryptionPreference() {
        return this.encryptionPreference;
    }
}
