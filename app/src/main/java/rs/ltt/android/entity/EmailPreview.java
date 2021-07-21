package rs.ltt.android.entity;

import androidx.room.Relation;

import com.google.common.base.Objects;

import java.time.Instant;
import java.util.List;

public abstract class EmailPreview extends EmailWithKeywords {

    public String threadId;
    public Instant receivedAt;

    @Relation(entity = EmailEmailAddressEntity.class, parentColumn = "id", entityColumn = "emailId", projection = {"email", "name", "type"})
    public List<EmailAddress> emailAddresses;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EmailPreview that = (EmailPreview) o;
        return Objects.equal(threadId, that.threadId) &&
                Objects.equal(receivedAt, that.receivedAt) &&
                Objects.equal(emailAddresses, that.emailAddresses);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), threadId, receivedAt, emailAddresses);
    }
}
