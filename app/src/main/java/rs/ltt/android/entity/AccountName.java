package rs.ltt.android.entity;

import com.google.common.base.Objects;
import rs.ltt.jmap.mua.util.AccountUtil;
import rs.ltt.jmap.mua.util.Navigable;

public class AccountName implements Navigable {

    public Long id;
    public String name;

    public String getName() {
        return this.name;
    }

    public String getPrintableName() {
        return AccountUtil.printableName(name);
    }

    public Long getId() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountName that = (AccountName) o;
        return Objects.equal(id, that.id) && Objects.equal(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, name);
    }
}
