package rs.ltt.android.ui;

import com.google.common.collect.ImmutableList;
import java.util.List;
import rs.ltt.android.entity.MailboxOverviewItem;
import rs.ltt.jmap.common.entity.Role;

public class EmptyMailboxAction {

    private static final List<Role> EMPTY_WORTHY_ROLES = ImmutableList.of(Role.TRASH);
    private final Role role;
    private final int itemCount;

    public EmptyMailboxAction(Role role, int itemCount) {
        this.role = role;
        this.itemCount = itemCount;
    }

    public static boolean emptyWorthy(final MailboxOverviewItem mailbox) {
        return mailbox != null
                && EMPTY_WORTHY_ROLES.contains(mailbox.getRole())
                && mailbox.totalEmails > 0;
    }

    public int getItemCount() {
        return itemCount;
    }

    public Role getRole() {
        return role;
    }
}
