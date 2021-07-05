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

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import rs.ltt.jmap.mua.util.KeywordUtil;

public abstract class EmailWithTextBodies extends EmailPreview {

    //TODO remove preview. use body values
    public String preview;

    @Relation(parentColumn = "id", entityColumn = "emailId")
    public List<EmailBodyPartEntity> bodyPartEntities;

    @Relation(parentColumn = "id", entityColumn = "emailId")
    public List<EmailBodyValueEntity> bodyValueEntities;

    public boolean isDraft() {
        return KeywordUtil.draft(this);
    }

    public String getPreview() {
        return preview;
    }

    public From getFrom() {
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

    //TODO rename to getTextBodies and return List<String>
    public String getText() {
        final ArrayList<EmailBodyPartEntity> textBody = new ArrayList<>();
        for (EmailBodyPartEntity entity : bodyPartEntities) {
            if (entity.bodyPartType == EmailBodyPartType.TEXT_BODY) {
                textBody.add(entity);
            }
        }
        Collections.sort(textBody, (o1, o2) -> o1.position.compareTo(o2.position));
        EmailBodyPartEntity first = Iterables.getFirst(textBody, null);
        Map<String, EmailBodyValueEntity> map = Maps.uniqueIndex(bodyValueEntities, value -> value.partId);
        EmailBodyValueEntity value = map.get(first.partId);
        return value.value;
    }

    public Collection<String> getTo() {
        LinkedHashMap<String, String> toMap = new LinkedHashMap<>();
        for (EmailAddress emailAddress : emailAddresses) {
            if (emailAddress.type == EmailAddressType.TO || emailAddress.type == EmailAddressType.CC) {
                toMap.put(emailAddress.getEmail(), emailAddress.getName());
            }
        }
        return toMap.values();
    }
}
