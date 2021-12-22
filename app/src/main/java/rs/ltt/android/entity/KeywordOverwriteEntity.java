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

import static androidx.room.ForeignKey.CASCADE;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import java.util.Collection;

@Entity(
        tableName = "keyword_overwrite",
        primaryKeys = {"threadId", "keyword"},
        foreignKeys =
                @ForeignKey(
                        entity = ThreadEntity.class,
                        parentColumns = {"threadId"},
                        childColumns = {"threadId"},
                        onDelete = CASCADE))
public class KeywordOverwriteEntity {

    @NonNull public String threadId;
    @NonNull public String keyword;
    public boolean value;

    public KeywordOverwriteEntity(
            @NonNull String threadId, @NonNull String keyword, boolean value) {
        this.threadId = threadId;
        this.keyword = keyword;
        this.value = value;
    }

    public static KeywordOverwriteEntity getKeywordOverwrite(
            Collection<KeywordOverwriteEntity> keywordOverwriteEntities, String keyword) {
        for (KeywordOverwriteEntity keywordOverwriteEntity : keywordOverwriteEntities) {
            if (keyword.equals(keywordOverwriteEntity.keyword)) {
                return keywordOverwriteEntity;
            }
        }
        return null;
    }
}
