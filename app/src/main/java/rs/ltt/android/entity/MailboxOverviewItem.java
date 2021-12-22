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
import com.google.common.base.Objects;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.mua.util.LabelWithCount;

public class MailboxOverviewItem extends MailboxWithRoleAndName implements LabelWithCount {

    public String parentId;

    public Integer sortOrder;

    public Integer totalEmails;

    public Integer unreadThreads;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MailboxOverviewItem that = (MailboxOverviewItem) o;
        return Objects.equal(id, that.id)
                && Objects.equal(parentId, that.parentId)
                && Objects.equal(name, that.name)
                && role == that.role
                && Objects.equal(sortOrder, that.sortOrder)
                && Objects.equal(totalEmails, that.totalEmails)
                && Objects.equal(unreadThreads, that.unreadThreads);
    }

    @Override
    public Integer getCount() {
        return unreadThreads;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, parentId, name, role, sortOrder, totalEmails, unreadThreads);
    }

    @Override
    public Role getRole() {
        return this.role;
    }

    @NonNull
    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }
}
