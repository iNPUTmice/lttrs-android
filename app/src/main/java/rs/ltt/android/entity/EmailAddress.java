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

import com.google.common.base.Objects;

public class EmailAddress {
    public EmailAddressType type;
    public String email;
    public String name;

    public String getEmail() {
        return this.email;
    }

    public String getName() {
        return name == null ? email.split("@")[0] : name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailAddress that = (EmailAddress) o;
        return type == that.type
                && Objects.equal(email, that.email)
                && Objects.equal(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, email, name);
    }
}
