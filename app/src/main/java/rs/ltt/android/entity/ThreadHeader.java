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
import java.util.List;
import java.util.Set;
import rs.ltt.jmap.common.entity.Keyword;
import rs.ltt.jmap.mua.util.KeywordUtil;

public class ThreadHeader {

    public String subject;
    public String threadId;

    @Relation(parentColumn = "threadId", entityColumn = "threadId", entity = EmailEntity.class)
    public List<EmailWithKeywords> emailsWithKeywords;

    @Relation(parentColumn = "threadId", entityColumn = "threadId")
    public Set<KeywordOverwriteEntity> keywordOverwriteEntities;

    public boolean showAsFlagged() {
        final KeywordOverwriteEntity flaggedOverwrite =
                KeywordOverwriteEntity.getKeywordOverwrite(
                        keywordOverwriteEntities, Keyword.FLAGGED);
        return flaggedOverwrite != null
                ? flaggedOverwrite.value
                : KeywordUtil.anyHas(emailsWithKeywords, Keyword.FLAGGED);
    }
}
