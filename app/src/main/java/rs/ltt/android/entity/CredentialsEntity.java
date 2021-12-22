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

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.common.base.Objects;
import okhttp3.HttpUrl;

@Entity(tableName = "credentials")
public class CredentialsEntity {

    @PrimaryKey(autoGenerate = true)
    public Long id;

    public String username;
    public String password;
    public HttpUrl sessionResource;

    public CredentialsEntity(String username, String password, HttpUrl sessionResource) {
        this.username = username;
        this.password = password;
        this.sessionResource = sessionResource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CredentialsEntity that = (CredentialsEntity) o;
        return Objects.equal(id, that.id)
                && Objects.equal(username, that.username)
                && Objects.equal(password, that.password)
                && Objects.equal(sessionResource, that.sessionResource);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, username, password, sessionResource);
    }
}
