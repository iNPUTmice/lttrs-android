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
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Set;
import rs.ltt.jmap.common.entity.IdentifiableEmailWithMailboxIds;

public class EmailWithMailboxes implements IdentifiableEmailWithMailboxIds {

    public String id;

    @Relation(
            entity = EmailMailboxEntity.class,
            parentColumn = "id",
            entityColumn = "emailId",
            projection = {"mailboxId"})
    public Set<String> mailboxIds;

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public Map<String, Boolean> getMailboxIds() {
        return Maps.asMap(mailboxIds, id -> true);
    }
}
