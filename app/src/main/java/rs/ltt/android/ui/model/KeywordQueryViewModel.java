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

package rs.ltt.android.ui.model;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import rs.ltt.android.entity.QueryInfo;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mua.util.StandardQueries;

public class KeywordQueryViewModel extends AbstractQueryViewModel {

    private final LiveData<EmailQuery> emailQueryLiveData;

    private final String keyword;

    private KeywordQueryViewModel(
            final Application application, final long accountId, @NonNull final String keyword) {
        super(application, accountId);
        this.keyword = keyword;
        this.emailQueryLiveData =
                Transformations.map(
                        queryRepository.getTrashAndJunk(),
                        trashAndJunk -> StandardQueries.keyword(keyword, trashAndJunk));
        init();
    }

    public String getKeyword() {
        return this.keyword;
    }

    @Override
    protected LiveData<EmailQuery> getQuery() {
        return emailQueryLiveData;
    }

    @Override
    public QueryInfo getQueryInfo() {
        return new QueryInfo(queryRepository.getAccountId(), QueryInfo.Type.KEYWORD, keyword);
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final Application application;
        private final long accountId;
        private final String keyword;

        public Factory(
                @NonNull Application application, final long accountId, @NonNull String keyword) {
            this.application = application;
            this.accountId = accountId;
            this.keyword = keyword;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return modelClass.cast(new KeywordQueryViewModel(application, accountId, keyword));
        }
    }
}
