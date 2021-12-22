package rs.ltt.android.ui;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public final class RecyclerViews {

    private RecyclerViews() {
        throw new IllegalStateException("Do not instantiate me");
    }

    public static boolean scrolledToTop(final RecyclerView recyclerView) {
        final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findFirstCompletelyVisibleItemPosition()
                    == 0;
        } else {
            return false;
        }
    }

    public static int findFirstVisibleItemPosition(final RecyclerView recyclerView) {
        final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
        } else {
            return RecyclerView.NO_POSITION;
        }
    }
}
