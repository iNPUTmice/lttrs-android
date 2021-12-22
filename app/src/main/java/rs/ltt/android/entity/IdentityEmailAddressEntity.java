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
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.common.collect.ImmutableList;
import java.util.List;
import rs.ltt.jmap.common.entity.EmailAddress;
import rs.ltt.jmap.common.entity.Identity;

@Entity(
        tableName = "identity_email_address",
        indices = {@Index(value = "identityId")},
        foreignKeys =
                @ForeignKey(
                        entity = IdentityEntity.class,
                        parentColumns = {"id"},
                        childColumns = {"identityId"},
                        onDelete = ForeignKey.CASCADE))
public class IdentityEmailAddressEntity {

    @PrimaryKey public Long id;

    @NonNull public String identityId;

    @NonNull public EmailAddressType type;

    public String name;
    public String email;

    public IdentityEmailAddressEntity(
            @NonNull String identityId, @NonNull EmailAddressType type, String name, String email) {
        this.identityId = identityId;
        this.type = type;
        this.name = name;
        this.email = email;
    }

    private static void addToBuilder(
            final ImmutableList.Builder<IdentityEmailAddressEntity> builder,
            final String identityId,
            final EmailAddressType type,
            List<EmailAddress> addresses) {
        for (EmailAddress address : addresses) {
            builder.add(
                    new IdentityEmailAddressEntity(
                            identityId, type, address.getName(), address.getEmail()));
        }
    }

    public static List<IdentityEmailAddressEntity> of(final Identity identity) {
        final List<EmailAddress> bcc = identity.getBcc();
        final List<EmailAddress> replyTo = identity.getReplyTo();
        final ImmutableList.Builder<IdentityEmailAddressEntity> builder =
                new ImmutableList.Builder<>();
        if (bcc != null) {
            addToBuilder(builder, identity.getId(), EmailAddressType.BCC, bcc);
        }
        if (replyTo != null) {
            addToBuilder(builder, identity.getId(), EmailAddressType.REPLY_TO, replyTo);
        }
        return builder.build();
    }
}
