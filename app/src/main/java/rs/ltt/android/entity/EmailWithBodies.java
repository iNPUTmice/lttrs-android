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
import com.google.common.base.Objects;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import rs.ltt.android.util.TextBodies;
import rs.ltt.jmap.mua.util.KeywordUtil;

/**
 * This e-mail model represents and individual e-mail in the thread view. It does not have a subject
 * because the subject is displayed once for the entire thread.
 */
public class EmailWithBodies extends EmailPreview {

    @Relation(parentColumn = "id", entityColumn = "emailId")
    public List<EmailBodyPartEntity> bodyPartEntities;

    @Relation(parentColumn = "id", entityColumn = "emailId")
    public List<EmailBodyValueEntity> bodyValueEntities;

    public boolean isDraft() {
        return KeywordUtil.draft(this);
    }

    public Preview getPreview() {
        final String preview = TextBodies.getPreview(bodyPartEntities, bodyValueEntities);
        return new Preview(preview, isEncrypted());
    }

    public From getFirstFrom() {
        if (KeywordUtil.draft(this)) {
            return From.draft();
        }
        for (final EmailAddress emailAddress : emailAddresses) {
            if (emailAddress.type == EmailAddressType.FROM) {
                return From.named(emailAddress);
            }
        }
        return null;
    }

    public List<String> getTextBodies() {
        return TextBodies.getTextBodies(bodyPartEntities, bodyValueEntities);
    }

    public List<EmailBodyPartEntity> getAttachments() {
        return EmailBodyPartEntity.filter(bodyPartEntities, EmailBodyPartType.ATTACHMENT);
    }

    public Collection<String> getToAndCc() {
        LinkedHashMap<String, String> toMap = new LinkedHashMap<>();
        for (EmailAddress emailAddress : emailAddresses) {
            if (emailAddress.type == EmailAddressType.TO
                    || emailAddress.type == EmailAddressType.CC) {
                toMap.put(emailAddress.getEmail(), emailAddress.getName());
            }
        }
        return toMap.values();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EmailWithBodies that = (EmailWithBodies) o;
        return Objects.equal(bodyPartEntities, that.bodyPartEntities)
                && Objects.equal(bodyValueEntities, that.bodyValueEntities);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), bodyPartEntities, bodyValueEntities);
    }
}
