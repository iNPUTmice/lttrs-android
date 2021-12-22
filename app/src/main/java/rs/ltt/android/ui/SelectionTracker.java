package rs.ltt.android.ui;

import java.util.Iterator;
import java.util.Set;
import rs.ltt.android.ui.adapter.ThreadOverviewAdapter;

public class SelectionTracker {

    private final Set<String> dataSource;
    private final ThreadOverviewAdapter adapter;
    private final OnSelectionChanged onSelectionChanged;

    public SelectionTracker(
            final Set<String> dataSource,
            final ThreadOverviewAdapter adapter,
            final OnSelectionChanged onSelectionChanged) {
        this.dataSource = dataSource;
        this.adapter = adapter;
        this.onSelectionChanged = onSelectionChanged;
        this.adapter.setSelectedThreads(dataSource);
    }

    public void select(final String threadId) {
        if (dataSource.add(threadId)) {
            adapter.notifyItemChanged(threadId);
            onSelectionChanged.onSelectionChanged();
        }
    }

    public void deselect(final String threadId) {
        if (dataSource.remove(threadId)) {
            adapter.notifyItemChanged(threadId);
            onSelectionChanged.onSelectionChanged();
        }
    }

    public boolean hasSelection() {
        return !this.dataSource.isEmpty();
    }

    public Set<String> getSelection() {
        return this.dataSource;
    }

    public void clearSelection() {
        final boolean hasSelection = hasSelection();
        final Iterator<String> iterator = dataSource.iterator();
        while (iterator.hasNext()) {
            final String threadId = iterator.next();
            iterator.remove();
            adapter.notifyItemChanged(threadId);
        }
        if (hasSelection) {
            onSelectionChanged.onSelectionChanged();
        }
    }

    public interface OnSelectionChanged {
        void onSelectionChanged();
    }
}
