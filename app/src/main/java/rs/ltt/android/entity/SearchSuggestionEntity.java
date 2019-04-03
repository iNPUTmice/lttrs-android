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
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "search_suggestion", indices = {@Index(value = {"query"}, unique = true)})
public class SearchSuggestionEntity {

    @PrimaryKey
    public Long id;

    public String query;

    public static SearchSuggestionEntity of(String query) {
        SearchSuggestionEntity entity = new SearchSuggestionEntity();
        entity.query = query;
        return entity;
    }

}
