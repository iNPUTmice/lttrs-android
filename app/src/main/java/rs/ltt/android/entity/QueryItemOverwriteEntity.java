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
import androidx.room.Index;

@Entity(
        tableName = "query_item_overwrite",
        primaryKeys = {"queryId", "threadId"},
        indices = {@Index(value = {"threadId"})},
        foreignKeys = {
            @ForeignKey(
                    entity = ThreadEntity.class,
                    parentColumns = {"threadId"},
                    childColumns = {"threadId"},
                    onDelete = CASCADE),
            @ForeignKey(
                    entity = QueryEntity.class,
                    parentColumns = {"id"},
                    childColumns = {"queryId"},
                    onDelete = CASCADE)
        })
public class QueryItemOverwriteEntity {

    @NonNull public Long queryId;
    @NonNull public String threadId;

    @NonNull public Type type;

    public boolean executed = false;

    public QueryItemOverwriteEntity(
            @NonNull Long queryId, @NonNull String threadId, @NonNull Type type) {
        this.queryId = queryId;
        this.threadId = threadId;
        this.type = type;
    }

    // TODO add one for IMPORTANT because that can be toggled individual as well
    public enum Type {
        KEYWORD,
        MAILBOX,
        SEARCH,
        OTHER
    }
}
