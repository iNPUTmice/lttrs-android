package rs.ltt.android.entity;

import androidx.room.Relation;
import com.google.common.base.Objects;
import com.google.common.collect.Collections2;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import rs.ltt.jmap.common.entity.IdentifiableEmailWithAddressesAndTime;
import rs.ltt.jmap.mua.util.EmailUtil;

/**
 * This e-mail model has keywords and addresses. It acts as a common base class for EmailWithBodies
 * (used in the Thread view) and EmailPreviewWithMailboxes (Used by the ThreadOverviewItem)
 */
public abstract class EmailPreview extends EmailWithKeywords
        implements IdentifiableEmailWithAddressesAndTime {

    public String threadId;
    public Instant receivedAt;
    public OffsetDateTime sentAt;
    public EncryptionStatus encryptionStatus;

    @Relation(
            entity = EmailEmailAddressEntity.class,
            parentColumn = "id",
            entityColumn = "emailId",
            projection = {"email", "name", "type"})
    public List<EmailAddress> emailAddresses;

    public EncryptionStatus getEncryptionStatus() {
        return this.encryptionStatus == null ? EncryptionStatus.CLEARTEXT : this.encryptionStatus;
    }

    @Override
    public Collection<rs.ltt.jmap.common.entity.EmailAddress> getSender() {
        return getAddresses(EmailAddressType.SENDER);
    }

    @Override
    public Collection<rs.ltt.jmap.common.entity.EmailAddress> getFrom() {
        return getAddresses(EmailAddressType.FROM);
    }

    public Collection<rs.ltt.jmap.common.entity.EmailAddress> getTo() {
        return getAddresses(EmailAddressType.TO);
    }

    @Override
    public Collection<rs.ltt.jmap.common.entity.EmailAddress> getCc() {
        return getAddresses(EmailAddressType.CC);
    }

    @Override
    public Collection<rs.ltt.jmap.common.entity.EmailAddress> getBcc() {
        return getAddresses(EmailAddressType.BCC);
    }

    @Override
    public Collection<rs.ltt.jmap.common.entity.EmailAddress> getReplyTo() {
        return getAddresses(EmailAddressType.REPLY_TO);
    }

    private Collection<rs.ltt.jmap.common.entity.EmailAddress> getAddresses(
            final EmailAddressType type) {
        return Collections2.transform(
                Collections2.filter(emailAddresses, input -> input != null && input.type == type),
                input ->
                        input == null
                                ? null
                                : rs.ltt.jmap.common.entity.EmailAddress.builder()
                                        .email(input.email)
                                        .name(input.name)
                                        .build());
    }

    public boolean isEncrypted() {
        return getEncryptionStatus().isEncrypted();
    }

    @Override
    public OffsetDateTime getSentAt() {
        return this.sentAt;
    }

    @Override
    public Instant getReceivedAt() {
        return this.receivedAt;
    }

    public Instant getEffectiveDate() {
        return EmailUtil.getEffectiveDate(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EmailPreview that = (EmailPreview) o;
        return Objects.equal(threadId, that.threadId)
                && Objects.equal(receivedAt, that.receivedAt)
                && Objects.equal(sentAt, that.sentAt)
                && encryptionStatus == that.encryptionStatus
                && Objects.equal(emailAddresses, that.emailAddresses);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                super.hashCode(), threadId, receivedAt, sentAt, encryptionStatus, emailAddresses);
    }
}
