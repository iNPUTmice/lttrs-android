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
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import rs.ltt.jmap.common.entity.Attachment;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.EmailBodyPart;

@Entity(
        tableName = "email_body_part",
        primaryKeys = {"emailId", "bodyPartType", "position"},
        foreignKeys =
                @ForeignKey(
                        entity = EmailEntity.class,
                        parentColumns = {"id"},
                        childColumns = {"emailId"},
                        onDelete = ForeignKey.CASCADE))
public class EmailBodyPartEntity implements Attachment, Comparable<EmailBodyPartEntity> {

    @NonNull public String emailId;
    @NonNull public EmailBodyPartType bodyPartType;

    public long position;
    public String partId;
    public String blobId;
    public Long size;
    public String name;
    public String type;
    public String charset;
    public String disposition;
    public String cid;
    public int downloadCount;

    public static List<EmailBodyPartEntity> of(Email email) {
        final ImmutableList.Builder<EmailBodyPartEntity> builder = new ImmutableList.Builder<>();
        final List<EmailBodyPart> textBody = nullToEmpty(email.getTextBody());
        for (int i = 0; i < textBody.size(); ++i) {
            builder.add(of(email.getId(), EmailBodyPartType.TEXT_BODY, i, textBody.get(i)));
        }
        final List<EmailBodyPart> attachment = nullToEmpty(email.getAttachments());
        for (int i = 0; i < attachment.size(); ++i) {
            builder.add(of(email.getId(), EmailBodyPartType.ATTACHMENT, i, attachment.get(i)));
        }
        return builder.build();
    }

    private static <T> List<T> nullToEmpty(final List<T> value) {
        return value == null ? Collections.emptyList() : value;
    }

    private static EmailBodyPartEntity of(
            String emailId, EmailBodyPartType type, long position, EmailBodyPart emailBodyPart) {
        final EmailBodyPartEntity entity = new EmailBodyPartEntity();
        entity.emailId = emailId;
        entity.bodyPartType = type;
        entity.position = position;
        entity.partId = emailBodyPart.getPartId();
        entity.blobId = emailBodyPart.getBlobId();
        entity.size = emailBodyPart.getSize();
        entity.name = emailBodyPart.getName();
        entity.type = emailBodyPart.getType();
        entity.charset = emailBodyPart.getCharset();
        entity.disposition = emailBodyPart.getDisposition();
        entity.cid = emailBodyPart.getCid();
        entity.downloadCount = 0;
        return entity;
    }

    public static List<EmailBodyPartEntity> filter(
            final List<EmailBodyPartEntity> entities, final EmailBodyPartType type) {
        return entities.stream()
                .filter(bp -> bp.bodyPartType == type)
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public String getBlobId() {
        return this.blobId;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getCharset() {
        return charset;
    }

    @Override
    public Long getSize() {
        return this.size;
    }

    @Override
    public int compareTo(final EmailBodyPartEntity o) {
        return Long.compare(position, o.position);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailBodyPartEntity that = (EmailBodyPartEntity) o;
        return position == that.position
                && Objects.equal(emailId, that.emailId)
                && bodyPartType == that.bodyPartType
                && Objects.equal(partId, that.partId)
                && Objects.equal(blobId, that.blobId)
                && Objects.equal(size, that.size)
                && Objects.equal(name, that.name)
                && Objects.equal(type, that.type)
                && Objects.equal(charset, that.charset)
                && Objects.equal(disposition, that.disposition)
                && Objects.equal(cid, that.cid)
                && Objects.equal(downloadCount, that.downloadCount);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                emailId,
                bodyPartType,
                position,
                partId,
                blobId,
                size,
                name,
                type,
                charset,
                disposition,
                cid,
                downloadCount);
    }
}
