package rs.ltt.android.entity;

import androidx.room.Relation;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Set;
import rs.ltt.jmap.common.entity.IdentifiableEmailWithMailboxIds;

public class EmailPreviewWithMailboxes extends EmailPreview
        implements IdentifiableEmailWithMailboxIds {

    public String preview;
    public String subject;

    @Relation(
            entity = EmailMailboxEntity.class,
            parentColumn = "id",
            entityColumn = "emailId",
            projection = {"mailboxId"})
    public Set<String> mailboxes;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailPreviewWithMailboxes email = (EmailPreviewWithMailboxes) o;
        return Objects.equal(id, email.id)
                && Objects.equal(preview, email.preview)
                && Objects.equal(threadId, email.threadId)
                && Objects.equal(subject, email.subject)
                && Objects.equal(receivedAt, email.receivedAt)
                && Objects.equal(sentAt, email.sentAt)
                && Objects.equal(keywords, email.keywords)
                && Objects.equal(emailAddresses, email.emailAddresses);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                id, preview, threadId, subject, receivedAt, sentAt, keywords, emailAddresses);
    }

    @Override
    public Map<String, Boolean> getKeywords() {
        return Maps.asMap(keywords, keyword -> true);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Map<String, Boolean> getMailboxIds() {
        return Maps.asMap(mailboxes, id -> true);
    }
}
