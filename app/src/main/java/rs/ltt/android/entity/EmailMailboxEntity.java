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
import androidx.room.Entity;
import androidx.room.ForeignKey;
import com.google.common.collect.ImmutableList;
import java.util.List;
import rs.ltt.jmap.common.entity.Email;

@Entity(
        tableName = "email_mailbox",
        primaryKeys = {"emailId", "mailboxId"},
        foreignKeys =
                @ForeignKey(
                        entity = EmailEntity.class,
                        parentColumns = {"id"},
                        childColumns = {"emailId"},
                        onDelete = ForeignKey.CASCADE))
public class EmailMailboxEntity {

    @NonNull public String emailId;

    @NonNull public String mailboxId;

    public EmailMailboxEntity(@NonNull String emailId, @NonNull String mailboxId) {
        this.emailId = emailId;
        this.mailboxId = mailboxId;
    }

    public static List<EmailMailboxEntity> of(Email email) {
        ImmutableList.Builder<EmailMailboxEntity> builder = new ImmutableList.Builder<>();
        for (String mailboxId : email.getMailboxIds().keySet()) {
            builder.add(new EmailMailboxEntity(email.getId(), mailboxId));
        }
        return builder.build();
    }
}
