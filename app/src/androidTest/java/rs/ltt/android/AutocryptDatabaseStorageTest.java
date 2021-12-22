package rs.ltt.android;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rs.ltt.android.cache.AutocryptDatabaseStorage;
import rs.ltt.android.database.LttrsDatabase;
import rs.ltt.autocrypt.client.Decision;
import rs.ltt.autocrypt.client.header.EncryptionPreference;
import rs.ltt.autocrypt.client.state.PeerStateManager;
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

    @After
    public void closeDatabase() {
        this.lttrsDatabase.close();
    }
}
