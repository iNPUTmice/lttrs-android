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

import androidx.room.Ignore;
import androidx.room.Relation;
import java.util.List;

/**
 * This e-mail model is used in the ComposeActivity. It contains all information necessary to edit a
 * draft including subject, body, all attachments, addresses and message Id as well as reply-to ids.
 */
public class EmailWithReferences extends EmailWithBodiesAndSubject
        implements IdentifiableWithOwner {

    public Long accountId;

    @Relation(
            entity = EmailInReplyToEntity.class,
            parentColumn = "id",
            entityColumn = "emailId",
            projection = {"id"})
    public List<String> inReplyTo;

    @Relation(
            entity = EmailMessageIdEntity.class,
            parentColumn = "id",
            entityColumn = "emailId",
            projection = {"id"})
    public List<String> messageId;

    @Relation(
            entity = ThreadItemEntity.class,
            parentColumn = "threadId",
            entityColumn = "threadId",
            projection = {"emailId"})
    public List<String> emailsInThread;

    @Ignore public List<String> identityEmailAddresses;

    public boolean isOnlyEmailInThread() {
        return emailsInThread != null && emailsInThread.size() == 1 && emailsInThread.contains(id);
    }

    @Override
    public Long getAccountId() {
        return accountId;
    }

    // TODO do something smarter to deal with someone editing an HTML email or an email with
    // multiple bodies
    public String getText() {
        return getTextBodies().stream().findAny().orElse("");
    }
}
