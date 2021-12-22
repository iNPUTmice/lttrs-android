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

@Entity(
        tableName = "account",
        foreignKeys = {
            @ForeignKey(
                    entity = CredentialsEntity.class,
                    parentColumns = {"id"},
                    childColumns = {"credentialsId"},
                    onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index(value = "credentialsId")})
public class AccountEntity {

    @PrimaryKey(autoGenerate = true)
    public Long id;

    @NonNull public long credentialsId;

    public String accountId;

    public String name;

    public boolean selected;

    public AccountEntity(@NonNull Long credentialsId, String accountId, String name) {
        this.credentialsId = credentialsId;
        this.accountId = accountId;
        this.name = name;
        this.selected = false;
    }
}
