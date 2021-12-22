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

import static androidx.room.ForeignKey.CASCADE;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import com.google.common.base.Objects;
import com.google.common.collect.Collections2;
import java.util.Collection;
import java.util.List;
import rs.ltt.android.util.CharSequences;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRoleAndName;
import rs.ltt.jmap.common.entity.Role;

@Entity(
        tableName = "mailbox_overwrite",
        primaryKeys = {"threadId", "name", "role"},
        foreignKeys =
                @ForeignKey(
                        entity = ThreadEntity.class,
                        parentColumns = {"threadId"},
                        childColumns = {"threadId"},
                        onDelete = CASCADE))
public class MailboxOverwriteEntity {

    @NonNull public String threadId;
    @NonNull public String name;
    @NonNull public String role;
    public boolean value;

    public static Collection<MailboxOverwriteEntity> of(
            Collection<String> threadIds, @NonNull Role role, boolean value) {
        return Collections2.transform(threadIds, threadId -> of(threadId, role, value));
    }

    public static MailboxOverwriteEntity of(String threadId, @NonNull Role role, boolean value) {
        MailboxOverwriteEntity entity = new MailboxOverwriteEntity();
        entity.threadId = threadId;
        entity.role = role.toString();
        entity.name = CharSequences.EMPTY_STRING;
        entity.value = value;
        return entity;
    }

    public static Collection<MailboxOverwriteEntity> of(
            Collection<String> threadIds, @NonNull String label, boolean value) {
        return Collections2.transform(threadIds, threadId -> of(threadId, label, value));
    }

    public static MailboxOverwriteEntity of(String threadId, @NonNull String label, boolean value) {
        MailboxOverwriteEntity entity = new MailboxOverwriteEntity();
        entity.threadId = threadId;
        entity.role = CharSequences.EMPTY_STRING;
        entity.name = label;
        entity.value = value;
        return entity;
    }

    public static boolean hasOverwrite(
            Collection<MailboxOverwriteEntity> overwriteEntities, Role role) {
        MailboxOverwriteEntity mailboxOverwriteEntity = find(overwriteEntities, role);
        if (mailboxOverwriteEntity != null) {
            return mailboxOverwriteEntity.value;
        }
        return false;
    }

    public static MailboxOverwriteEntity find(
            Collection<MailboxOverwriteEntity> overwriteEntities, Role role) {
        for (MailboxOverwriteEntity overwriteEntity : overwriteEntities) {
            if (role.toString().equals(overwriteEntity.role)) {
                return overwriteEntity;
            }
        }
        return null;
    }

    public static Boolean getOverwrite(
            final List<MailboxOverwriteEntity> mailboxOverwrites,
            IdentifiableMailboxWithRoleAndName mailbox) {
        for (final MailboxOverwriteEntity overwrite : mailboxOverwrites) {
            if (overwrite.matches(mailbox)) {
                return overwrite.value;
            }
        }
        return null;
    }

    public boolean matches(final IdentifiableMailboxWithRoleAndName mailbox) {
        final Role role = mailbox.getRole();
        final String name = mailbox.getName();
        if (role != null) {
            return role.toString().equals(this.role);
        } else if (name != null) {
            return name.equals(this.name);
        } else {
            return false;
        }
    }

    public boolean matches(
            final Collection<? extends IdentifiableMailboxWithRoleAndName> mailboxes) {
        for (final IdentifiableMailboxWithRoleAndName mailbox : mailboxes) {
            if (matches(mailbox)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MailboxOverwriteEntity entity = (MailboxOverwriteEntity) o;
        return value == entity.value
                && Objects.equal(threadId, entity.threadId)
                && Objects.equal(name, entity.name)
                && Objects.equal(role, entity.role);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(threadId, name, role, value);
    }
}
