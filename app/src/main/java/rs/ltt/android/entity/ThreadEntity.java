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
import androidx.room.PrimaryKey;
import rs.ltt.jmap.common.entity.Thread;

@Entity(tableName = "thread")
public class ThreadEntity {

    @PrimaryKey @NonNull public String threadId;

    public ThreadEntity(@NonNull String threadId) {
        this.threadId = threadId;
    }

    public static ThreadEntity of(Thread thread) {
        return new ThreadEntity(thread.getId());
    }

    public static ThreadEntity of(String id) {
        return new ThreadEntity(id);
    }
}
