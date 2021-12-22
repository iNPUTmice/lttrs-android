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

package rs.ltt.android.database.dao;

import static androidx.room.OnConflictStrategy.REPLACE;

import android.app.SearchManager;
import android.database.Cursor;
import android.provider.BaseColumns;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;
import rs.ltt.android.R;
import rs.ltt.android.entity.SearchSuggestionEntity;

@Dao
public abstract class SearchSuggestionDao {

    @Query(
            "select id as "
                    + BaseColumns._ID
                    + ", `query` as "
                    + SearchManager.SUGGEST_COLUMN_TEXT_1
                    + ","
                    + R.drawable.ic_restore_24dp
                    + " as "
                    + SearchManager.SUGGEST_COLUMN_ICON_1
                    + ",`query` as "
                    + SearchManager.SUGGEST_COLUMN_QUERY
                    + " from search_suggestion where `query` like :needle and `query` is not"
                    + " :actual order by id desc limit 30")
    abstract Cursor getSearchSuggestions(String needle, String actual);

    public Cursor getSearchSuggestions(String needle) {
        return getSearchSuggestions('%' + needle + (needle.isEmpty() ? "" : "%"), needle);
    }

    @Insert(onConflict = REPLACE)
    public abstract void insert(SearchSuggestionEntity entity);

    @Query("select `query` from search_suggestion")
    public abstract List<String> getSearchQueries();
}
