package rs.ltt.android.ui;

import com.google.common.base.Objects;
import java.util.Arrays;
import java.util.List;
import rs.ltt.jmap.mua.util.Navigable;

public class AdditionalNavigationItem implements Navigable {

    public static final List<AdditionalNavigationItem> ACCOUNT_SELECTOR_ITEMS =
            Arrays.asList(
                    new AdditionalNavigationItem(Type.ADD_ACCOUNT),
                    new AdditionalNavigationItem(Type.MANAGE_ACCOUNT));

    public final Type type;

    public AdditionalNavigationItem(final Type type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdditionalNavigationItem that = (AdditionalNavigationItem) o;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type);
    }

    public enum Type {
        MANAGE_ACCOUNT,
        ADD_ACCOUNT
    }
}
