package rs.ltt.android.ui;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class ExtendedFabSizeChanger extends RecyclerView.OnScrollListener {

    private final ExtendedFloatingActionButton extendedFloatingActionButton;

    private ExtendedFabSizeChanger(
            final ExtendedFloatingActionButton extendedFloatingActionButton) {
        this.extendedFloatingActionButton = extendedFloatingActionButton;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        if (RecyclerViews.findFirstVisibleItemPosition(recyclerView) > 0) {
            extendedFloatingActionButton.shrink();
        } else {
            extendedFloatingActionButton.extend();
        }
    }

    public static RecyclerView.OnScrollListener of(final ExtendedFloatingActionButton fab) {
        return new ExtendedFabSizeChanger(fab);
    }
}
