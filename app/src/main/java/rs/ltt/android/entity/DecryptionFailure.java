package rs.ltt.android.entity;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.List;
import javax.net.ssl.SSLException;
import org.pgpainless.exception.MissingDecryptionMethodException;
import rs.ltt.android.worker.Failure;

public class DecryptionFailure {

    private static final List<Class<? extends Exception>> NETWORK_EXCEPTIONS =
            ImmutableList.of(
                    SocketTimeoutException.class, SocketException.class, SSLException.class);

    public final List<Failure> missingDecryption;
    public final List<Failure> networkFailure;
    public final List<Failure> other;

    public DecryptionFailure(final Collection<Failure> failures) {
        final ImmutableList.Builder<Failure> missingDecryptionBuilder = ImmutableList.builder();
        final ImmutableList.Builder<Failure> networkFailureBuilder = ImmutableList.builder();
        final ImmutableList.Builder<Failure> otherBuilder = ImmutableList.builder();
        for (final Failure failure : failures) {
            if (failure.getException().equals(MissingDecryptionMethodException.class)) {
                missingDecryptionBuilder.add(failure);
            } else if (NETWORK_EXCEPTIONS.contains(failure.getException())) {
                networkFailureBuilder.add(failure);
            } else {
                otherBuilder.add(failure);
            }
        }
        this.missingDecryption = missingDecryptionBuilder.build();
        this.networkFailure = networkFailureBuilder.build();
        this.other = otherBuilder.build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("missingDecryption", missingDecryption)
                .add("networkFailure", networkFailure)
                .add("other", other)
                .toString();
    }
}
