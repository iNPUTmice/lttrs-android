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

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRoleAndName;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.Role;

@Entity(tableName = "mailbox")
public class MailboxEntity implements IdentifiableMailboxWithRoleAndName {

    @NonNull @PrimaryKey public String id;

    public String name;

    public String parentId;

    public Role role;

    public Long sortOrder;

    public Long totalEmails;

    public Long unreadEmails;

    public Long totalThreads;

    public Long unreadThreads;

    @Embedded public MailboxRightsEmbed myRights;

    public Boolean isSubscribed;

    public static MailboxEntity of(final Mailbox mailbox) {
        final MailboxEntity entity = new MailboxEntity();
        entity.id = mailbox.getId();
        entity.name = mailbox.getName();
        entity.parentId = mailbox.getParentId();
        entity.role = mailbox.getRole();
        entity.sortOrder = mailbox.getSortOrder();
        entity.totalEmails = mailbox.getTotalEmails();
        entity.unreadEmails = mailbox.getUnreadEmails();
        entity.totalThreads = mailbox.getTotalThreads();
        entity.unreadThreads = mailbox.getUnreadThreads();
        entity.myRights = MailboxRightsEmbed.of(mailbox.getMyRights());
        entity.isSubscribed = mailbox.getIsSubscribed();
        return entity;
    }

    @Override
    @NonNull
    public String getId() {
        return this.id;
    }

    @Override
    public Role getRole() {
        return role;
    }

    @Override
    public String getName() {
        return name;
    }
}
