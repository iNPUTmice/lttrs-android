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
import java.util.List;
import java.util.Map;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.EmailBodyValue;

@Entity(
        tableName = "email_body_value",
        primaryKeys = {"emailId", "partId"},
        foreignKeys =
                @ForeignKey(
                        entity = EmailEntity.class,
                        parentColumns = {"id"},
                        childColumns = {"emailId"},
                        onDelete = ForeignKey.CASCADE))
public class EmailBodyValueEntity {

    @NonNull public String emailId;
    @NonNull public String partId;

    public String value;
    public Boolean isEncodingProblem;
    public Boolean isTruncated;

    public static List<EmailBodyValueEntity> of(Email email) {
        final ImmutableList.Builder<EmailBodyValueEntity> builder = new ImmutableList.Builder<>();
        for (final Map.Entry<String, EmailBodyValue> bodyValue : email.getBodyValues().entrySet()) {
            builder.add(of(email.getId(), bodyValue.getKey(), bodyValue.getValue()));
        }
        return builder.build();
    }

    private static EmailBodyValueEntity of(
            String emailId, String partId, EmailBodyValue emailBodyValue) {
        final EmailBodyValueEntity entity = new EmailBodyValueEntity();
        entity.emailId = emailId;
        entity.partId = partId;
        entity.value = emailBodyValue.getValue();
        entity.isEncodingProblem = emailBodyValue.getIsEncodingProblem();
        entity.isTruncated = emailBodyValue.getIsTruncated();
        return entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailBodyValueEntity that = (EmailBodyValueEntity) o;
        return Objects.equal(emailId, that.emailId)
                && Objects.equal(partId, that.partId)
                && Objects.equal(value, that.value)
                && Objects.equal(isEncodingProblem, that.isEncodingProblem)
                && Objects.equal(isTruncated, that.isTruncated);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(emailId, partId, value, isEncodingProblem, isTruncated);
    }
}
