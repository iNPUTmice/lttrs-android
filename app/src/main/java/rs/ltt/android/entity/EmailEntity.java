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
import androidx.room.PrimaryKey;
import com.google.common.base.Optional;
import java.time.Instant;
import java.time.OffsetDateTime;
import rs.ltt.autocrypt.jmap.EncryptedBodyPart;
import rs.ltt.jmap.common.entity.Downloadable;
import rs.ltt.jmap.common.entity.Email;

@Entity(tableName = "email")
public class EmailEntity {

    @NonNull @PrimaryKey public String id;

    public String blobId;

    public String threadId;

    public Long size;

    public Instant receivedAt;

    public String subject;

    public OffsetDateTime sentAt;

    public Boolean hasAttachment;

    public String preview;

    public EncryptionStatus encryptionStatus;

    public String encryptedBlobId;

    public static EmailEntity of(final Email email) {
        final Optional<Downloadable> encryptedBodyPart =
                EncryptedBodyPart.findEncryptedBodyPart(email);
        final EmailEntity entity = new EmailEntity();
        entity.id = email.getId();
        entity.blobId = email.getBlobId();
        entity.threadId = email.getThreadId();
        entity.size = email.getSize();
        entity.receivedAt = email.getReceivedAt();
        entity.subject = email.getSubject();
        entity.sentAt = email.getSentAt();
        entity.hasAttachment = email.getHasAttachment();
        entity.preview = email.getPreview();
        if (encryptedBodyPart.isPresent()) {
            entity.encryptionStatus = EncryptionStatus.ENCRYPTED;
            entity.encryptedBlobId = encryptedBodyPart.get().getBlobId();
        } else {
            entity.encryptionStatus = EncryptionStatus.CLEARTEXT;
        }
        return entity;
    }
}
