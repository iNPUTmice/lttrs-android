package rs.ltt.android;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.core.internal.deps.guava.collect.ImmutableList;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rs.ltt.android.cache.AutocryptDatabaseStorage;
import rs.ltt.android.database.LttrsDatabase;
import rs.ltt.autocrypt.client.Decision;
import rs.ltt.autocrypt.client.header.EncryptionPreference;
import rs.ltt.autocrypt.client.state.GossipUpdate;
import rs.ltt.autocrypt.client.state.PeerStateManager;
import rs.ltt.autocrypt.client.state.PreRecommendation;
import rs.ltt.autocrypt.client.storage.PeerState;
import rs.ltt.autocrypt.client.storage.Storage;

@RunWith(AndroidJUnit4.class)
public class AutocryptDatabaseStorageTest {

    private static final Instant EFFECTIVE_DATE_INITIAL = Instant.ofEpochSecond(1_500_000_000);
    private static final Instant EFFECTIVE_DATE_UPDATE = Instant.ofEpochSecond(1_600_000_000);
    private static final Instant EFFECTIVE_DATE_EARLIER_UPDATE =
            Instant.ofEpochSecond(1_550_000_000);

    private static final String EXAMPLE_HEADER =
            "addr=test@example.com; prefer-encrypt=nopreference; keydata=mDMEYayg9BYJKwYBBAHa\n"
                + "Rw8BAQdAXNE+WhE4MzTK8UYL9BPvXa4vvpTi91kyePuDsp3Zl660HFRlc3QgVGVzdCA8dGVzdEBleGFt\n"
                + "cGxlLmNvbT6IjwQTFgoAQQUCYayg9AmQ2bYzbbMLwX0WoQT9xNDMu1y5/5bSfW3ZtjNtswvBfQKeAQKb\n"
                + "AQWWAgMBAASLCQgHBZUKCQgLApkBAAClTQD7BlPx15g89a4xYaNnFKUfTAxKXjA5B9KO6stEwi2HDYgB\n"
                + "ANkakdV/VcdOMyklo75z6wGa3AlAvA9n+8fnj6/UkrUGuDgEYayg9BIKKwYBBAGXVQEFAQEHQPv1w6k2\n"
                + "ShWEvw1UCyrgCQbuzGQQzLSgquNGzb9qezwDAwEIB4h1BBgWCgAdBQJhrKD0Ap4BApsMBZYCAwEABIsJ\n"
                + "CAcFlQoJCAsACgkQ2bYzbbMLwX3jEgEAm02M1HktY8aGvNpKmSWXoTWOWRGIZxMA1NhAFS7ce9wA/2Ju\n"
                + "6EiQsDXARz6+yQRW3nhyTRcdNf27G+93SpLBd44HuDMEYayg9BYJKwYBBAHaRw8BAQdA6UJC37S+8myZ\n"
                + "kvwxFYDAFqCGJN6XE61d70i5GPiZTyuI1QQYFgoAfQUCYayg9AKeAQKbAgWWAgMBAASLCQgHBZUKCQgL\n"
                + "XyAEGRYKAAYFAmGsoPQACgkQEEFKC1yIxmuFRAD+OHKaq12Jj+OJokJiF8CDIe1NrpwdpOTYyN47+V3U\n"
                + "+5QBAMl07HdfYIXR5r5SaEQOgqLqtu5JnXL5xGv26DcGOXkNAAoJENm2M22zC8F9IiEA/RlT+sIaGbwq\n"
                + "KsAFDSqpRX5VR1/QzyfafS9qWfL93qyMAQCDwKyemcwRo2m7/dJ8b+oHQAFnhmp/nZyXeBB1xdCACA==";

    private static final String GOSSIP_UPDATE =
            "addr=test@example.com;"
                + " keydata=mDMEYekUDxYJKwYBBAHaRw8BAQdAy90O4ktCtK4L1ItmCA41HMBpGLxH4tJ6v5a/UTzp+pO0DTxiZXRhQGx0dC5ycz6IjwQTFgoAQQUCYekUDwmQORFGsYjt9LQWoQTgBxnxVt7Z+TtCIYI5EUaxiO30tAKeAQKbAwWWAgMBAASLCQgHBZUKCQgLApkBAACz4QEA8RbL034q6M8DnCPf1qNiGWr/s1qCVTPM7Z8A6Cra5WcBAJzMJFL+8XhEDFDn4V0DAHrfvw0JYKRuxydAF4t81Y0DuDgEYekUDxIKKwYBBAGXVQEFAQEHQOW5L58UXQDloq736qVTi9kyPUW4Xxy9eOc5hCr0DzBkAwEIB4h1BBgWCgAdBQJh6RQPAp4BApsMBZYCAwEABIsJCAcFlQoJCAsACgkQORFGsYjt9LTlZQEAtc8ynGu9nr9N9X58Ry9+AMBdUYtUxb+QRISiJS4Q8t4BAO4fJQ7u6vNYgOVDctyuATCatXyDRwzhf132yzHGtTkB";

    private LttrsDatabase lttrsDatabase;
    private Storage storage;

    @Before
    public void createDatabase() {
        this.lttrsDatabase =
                Room.inMemoryDatabaseBuilder(
                                ApplicationProvider.getApplicationContext(), LttrsDatabase.class)
                        .build();
        this.storage = new AutocryptDatabaseStorage(this.lttrsDatabase);
    }

    @Test
    public void processHeader() {
        final PeerStateManager peerStateManager = new PeerStateManager(storage);

        peerStateManager.processAutocryptHeaders(
                "test@example.com", EFFECTIVE_DATE_INITIAL, Collections.singleton(EXAMPLE_HEADER));
        final PeerState peerState = storage.getPeerState("test@example.com");
        Assert.assertNotNull(peerState);

        Assert.assertEquals(EFFECTIVE_DATE_INITIAL, peerState.getLastSeen());
        Assert.assertEquals(EFFECTIVE_DATE_INITIAL, peerState.getAutocryptTimestamp());
        Assert.assertEquals(
                EncryptionPreference.NO_PREFERENCE, peerState.getEncryptionPreference());
    }

    @Test
    public void processEmptyHeader() {
        final PeerStateManager peerStateManager = new PeerStateManager(storage);

        peerStateManager.processAutocryptHeaders(
                "test@example.com", EFFECTIVE_DATE_INITIAL, Collections.emptyList());
        final PeerState peerState = storage.getPeerState("test@example.com");
        Assert.assertNotNull(peerState);

        Assert.assertEquals(EFFECTIVE_DATE_INITIAL, peerState.getLastSeen());
        Assert.assertNull(peerState.getPublicKey());
    }

    @Test
    public void processHeaderAndUpdate() {
        final PeerStateManager peerStateManager = new PeerStateManager(storage);

        peerStateManager.processAutocryptHeaders(
                "test@example.com", EFFECTIVE_DATE_INITIAL, Collections.singleton(EXAMPLE_HEADER));

        peerStateManager.processAutocryptHeaders(
                "test@example.com", EFFECTIVE_DATE_UPDATE, Collections.emptyList());

        peerStateManager.processAutocryptHeaders(
                "test@example.com", EFFECTIVE_DATE_EARLIER_UPDATE, Collections.emptyList());

        final PeerState peerState = storage.getPeerState("test@example.com");
        Assert.assertNotNull(peerState);

        Assert.assertEquals(EFFECTIVE_DATE_UPDATE, peerState.getLastSeen());
        Assert.assertEquals(EFFECTIVE_DATE_INITIAL, peerState.getAutocryptTimestamp());

        Assert.assertNotNull(peerState.getPublicKey());
    }

    @Test
    public void preliminaryRecommendationAvailable() {
        final PeerStateManager peerStateManager = new PeerStateManager(this.storage);

        peerStateManager.processAutocryptHeaders(
                "test@example.com", EFFECTIVE_DATE_INITIAL, Collections.singleton(EXAMPLE_HEADER));

        Assert.assertEquals(
                Decision.AVAILABLE,
                peerStateManager.getPreliminaryRecommendation("test@example.com").getDecision());
    }

    @Test
    public void preliminaryRecommendationDiscourage() {
        final PeerStateManager peerStateManager = new PeerStateManager(this.storage);

        peerStateManager.processAutocryptHeaders(
                "test@example.com", EFFECTIVE_DATE_INITIAL, Collections.singleton(EXAMPLE_HEADER));

        peerStateManager.processAutocryptHeaders(
                "test@example.com",
                EFFECTIVE_DATE_UPDATE.plus(Duration.ofDays(90)),
                Collections.emptyList());

        Assert.assertEquals(
                Decision.DISCOURAGE,
                peerStateManager.getPreliminaryRecommendation("test@example.com").getDecision());
    }

    @Test
    public void preliminaryRecommendationDisabled() {
        final PeerStateManager peerStateManager = new PeerStateManager(this.storage);

        Assert.assertEquals(
                Decision.DISABLE,
                peerStateManager.getPreliminaryRecommendation("nobody@example.com").getDecision());
    }

    @Test
    public void processGossipAndAutocrypt() {
        final PeerStateManager peerStateManager = new PeerStateManager(this.storage);
        final List<GossipUpdate> updates =
                GossipUpdate.builder(EFFECTIVE_DATE_INITIAL).add(EXAMPLE_HEADER).build();
        peerStateManager.processGossipHeader(ImmutableList.of("test@example.com"), updates);
        Assert.assertEquals(
                Decision.DISCOURAGE,
                peerStateManager.getPreliminaryRecommendation("test@example.com").getDecision());

        peerStateManager.processAutocryptHeaders(
                "test@example.com", EFFECTIVE_DATE_INITIAL, Collections.singleton(EXAMPLE_HEADER));

        Assert.assertEquals(
                Decision.AVAILABLE,
                peerStateManager.getPreliminaryRecommendation("test@example.com").getDecision());
    }

    @Test
    public void processAutocryptAndGossip() {
        final PeerStateManager peerStateManager = new PeerStateManager(this.storage);
        final List<GossipUpdate> updates =
                GossipUpdate.builder(EFFECTIVE_DATE_INITIAL).add(EXAMPLE_HEADER).build();

        peerStateManager.processAutocryptHeaders(
                "test@example.com", EFFECTIVE_DATE_INITIAL, Collections.singleton(EXAMPLE_HEADER));

        Assert.assertEquals(
                Decision.AVAILABLE,
                peerStateManager.getPreliminaryRecommendation("test@example.com").getDecision());

        peerStateManager.processGossipHeader(ImmutableList.of("test@example.com"), updates);

        Assert.assertEquals(
                Decision.AVAILABLE,
                peerStateManager.getPreliminaryRecommendation("test@example.com").getDecision());
    }

    @Test
    public void updateGossip() throws IOException {
        final PeerStateManager peerStateManager = new PeerStateManager(this.storage);
        final List<GossipUpdate> updates =
                GossipUpdate.builder(EFFECTIVE_DATE_INITIAL).add(EXAMPLE_HEADER).build();

        peerStateManager.processGossipHeader(ImmutableList.of("test@example.com"), updates);

        final PreRecommendation preRecommendation =
                peerStateManager.getPreliminaryRecommendation("test@example.com");

        final PGPPublicKeyRing firstGossipKey = preRecommendation.getPublicKey();

        Assert.assertEquals(Decision.DISCOURAGE, preRecommendation.getDecision());

        // gossip key gets updated to the key referenced by the GOSSIP_UPDATE
        final List<GossipUpdate> moreUpdates =
                GossipUpdate.builder(EFFECTIVE_DATE_UPDATE).add(GOSSIP_UPDATE).build();
        peerStateManager.processGossipHeader(ImmutableList.of("test@example.com"), moreUpdates);

        final PreRecommendation secondPreRecommendation =
                peerStateManager.getPreliminaryRecommendation("test@example.com");

        final PGPPublicKeyRing secondGossipKey = secondPreRecommendation.getPublicKey();

        // check that first key and second key don't match. this means the gossip got updated
        Assert.assertFalse(
                Arrays.equals(firstGossipKey.getEncoded(), secondGossipKey.getEncoded()));

        // try to revert back to the old key. but this should fail due to the earlier timestamp
        peerStateManager.processGossipHeader(
                ImmutableList.of("test@example.com"),
                GossipUpdate.builder(EFFECTIVE_DATE_EARLIER_UPDATE).add(EXAMPLE_HEADER).build());

        final PGPPublicKeyRing thirdGossipKey =
                peerStateManager.getPreliminaryRecommendation("test@example.com").getPublicKey();

        Assert.assertArrayEquals(secondGossipKey.getEncoded(), thirdGossipKey.getEncoded());
    }

    @After
    public void closeDatabase() {
        this.lttrsDatabase.close();
    }
}
