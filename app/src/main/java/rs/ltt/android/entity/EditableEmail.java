/*
 * Copyright 2019 Daniel Gultsch
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rs.ltt.android.entity;

import androidx.room.Relation;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import rs.ltt.jmap.common.entity.IdentifiableEmailWithAddresses;
import rs.ltt.jmap.common.entity.IdentifiableEmailWithSubject;

//TODO extend EmailComplete; rename to EmailEditable
public class EditableEmail implements IdentifiableEmailWithAddresses, IdentifiableEmailWithSubject, IdentifiableWithOwner {

    public Long accountId;

    public String id;

    public String threadId;

    public String subject;

    @Relation(entity = EmailEmailAddressEntity.class, parentColumn = "id", entityColumn = "emailId", projection = {"email", "name", "type"})
    public List<EmailAddress> emailAddresses;

    @Relation(parentColumn = "id", entityColumn = "emailId")
    public List<EmailBodyPartEntity> bodyPartEntities;

    @Relation(parentColumn = "id", entityColumn = "emailId")
    public List<EmailBodyValueEntity> bodyValueEntities;

    @Relation(entity = EmailInReplyToEntity.class, parentColumn = "id", entityColumn = "emailId", projection = {"id"})
    public List<String> inReplyTo;

    @Relation(entity = EmailMessageIdEntity.class, parentColumn = "id", entityColumn = "emailId", projection = {"id"})
    public List<String> messageId;

    @Relation(entity = ThreadItemEntity.class, parentColumn = "threadId", entityColumn = "threadId", projection = {"emailId"})
    public List<String> emailsInThread;

    //TODO inheriting from EmailComplete will make this obsolete
    public String getText() {
        final ArrayList<EmailBodyPartEntity> textBody = new ArrayList<>();
        for (EmailBodyPartEntity entity : bodyPartEntities) {
            if (entity.bodyPartType == EmailBodyPartType.TEXT_BODY) {
                textBody.add(entity);
            }
        }
        //Collections.sort(textBody, (o1, o2) -> o1.position.compareTo(o2.position));
        EmailBodyPartEntity first = Iterables.getFirst(textBody, null);
        Map<String, EmailBodyValueEntity> map = Maps.uniqueIndex(bodyValueEntities, value -> value.partId);
        EmailBodyValueEntity value = map.get(first.partId);
        return value.value;
    }

    public boolean isOnlyEmailInThread() {
        return emailsInThread != null && emailsInThread.size() == 1 && emailsInThread.contains(id);
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

    private Collection<rs.ltt.jmap.common.entity.EmailAddress> getAddresses(final EmailAddressType type) {
        return Collections2.transform(
                Collections2.filter(
                        emailAddresses,
                        input -> input != null && input.type == type
                ),
                input -> input == null ? null : rs.ltt.jmap.common.entity.EmailAddress.builder().email(input.email).name(input.name).build()
        );
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Long getAccountId() {
        return accountId;
    }
}
