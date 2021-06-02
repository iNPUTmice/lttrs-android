package rs.ltt.android.entity;

import androidx.room.Relation;

import java.time.Instant;
import java.util.List;

public abstract class EmailPreview extends EmailWithKeywords {

    public String threadId;
    public Instant receivedAt;

    @Relation(entity = EmailEmailAddressEntity.class, parentColumn = "id", entityColumn = "emailId", projection = {"email", "name", "type"})
    public List<EmailAddress> emailAddresses;

}
