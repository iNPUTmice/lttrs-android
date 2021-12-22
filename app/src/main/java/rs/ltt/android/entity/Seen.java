package rs.ltt.android.entity;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;

public class Seen {

    private final boolean seen;
    private final List<ExpandedPosition> expandedPositions;

    private Seen(boolean seen, List<ExpandedPosition> expandedPositions) {
        this.seen = seen;
        this.expandedPositions = expandedPositions;
    }

    public static ListenableFuture<Seen> of(
            final boolean seen, ListenableFuture<List<ExpandedPosition>> expandedPortionsFuture) {
        return Futures.transform(
                expandedPortionsFuture, ex -> new Seen(seen, ex), MoreExecutors.directExecutor());
    }

    public boolean isUnread() {
        return !seen;
    }

    public List<ExpandedPosition> getExpandedPositions() {
        return expandedPositions;
    }
}
