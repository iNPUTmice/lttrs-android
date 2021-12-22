package rs.ltt.android.ui;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemAnimators {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemAnimators.class);

    public static void disableChangeAnimation(final RecyclerView.ItemAnimator itemAnimator) {
        if (itemAnimator instanceof SimpleItemAnimator) {
            final SimpleItemAnimator simpleItemAnimator = (SimpleItemAnimator) itemAnimator;
            simpleItemAnimator.setSupportsChangeAnimations(false);
        }
    }

    private static RecyclerView.ItemAnimator createDefault() {
        final DefaultItemAnimator defaultItemAnimator = new DefaultItemAnimator();
        disableChangeAnimation(defaultItemAnimator);
        return defaultItemAnimator;
    }

    public static void configureItemAnimator(
            final RecyclerView recyclerView, final boolean isInitialLoad) {
        final RecyclerView.ItemAnimator itemAnimator = recyclerView.getItemAnimator();
        if (isInitialLoad) {
            LOGGER.info("Disable item animator");
            recyclerView.setItemAnimator(null);
        } else if (itemAnimator == null) {
            LOGGER.info("Enable default item animator");
            recyclerView.setItemAnimator(ItemAnimators.createDefault());
        }
    }
}
