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

package rs.ltt.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import com.google.common.base.Preconditions;
import java.util.Collection;
import rs.ltt.android.entity.ThreadOverviewItem;
import rs.ltt.android.ui.ActionModeMenuConfiguration;
import rs.ltt.android.ui.QueryItemTouchHelper;
import rs.ltt.android.ui.model.AbstractQueryViewModel;
import rs.ltt.android.ui.model.SearchQueryViewModel;

public class SearchQueryFragment extends AbstractQueryFragment {

    private SearchQueryViewModel searchQueryViewModel;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        final String term =
                SearchQueryFragmentArgs.fromBundle(bundle == null ? new Bundle() : bundle)
                        .getText();
        final ViewModelProvider viewModelProvider =
                new ViewModelProvider(
                        getViewModelStore(),
                        new SearchQueryViewModel.Factory(
                                requireActivity().getApplication(),
                                getLttrsViewModel().getAccountId(),
                                term));
        this.searchQueryViewModel = viewModelProvider.get(SearchQueryViewModel.class);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected AbstractQueryViewModel getQueryViewModel() {
        return this.searchQueryViewModel;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        searchQueryViewModel
                .getSearchTerm()
                .observe(
                        getViewLifecycleOwner(),
                        searchTerm -> {
                            if (searchTerm == null) {
                                return;
                            }
                            getLttrsViewModel().setSelectedLabel(null);
                            getLttrsViewModel().setCurrentSearchTerm(searchTerm);
                        });
    }

    @Override
    protected QueryItemTouchHelper.Swipable onQueryItemSwipe(ThreadOverviewItem item) {
        return searchQueryViewModel.isInInbox(item)
                ? QueryItemTouchHelper.Swipable.ARCHIVE
                : QueryItemTouchHelper.Swipable.NO;
    }

    @Override
    protected boolean isQueryItemRemovedAfterSwipe() {
        return false;
    }

    @Override
    protected void onQueryItemSwiped(ThreadOverviewItem item) {
        Preconditions.checkState(
                searchQueryViewModel.isInInbox(item), "Swiped thread is not in inbox");
        archive(item);
    }

    @Override
    protected boolean showComposeButton() {
        return false;
    }

    @Override
    void removeLabel(Collection<String> threadIds) {
        throw new IllegalStateException("SearchQueryFragment should not offer remove label button");
    }

    @Override
    protected ActionModeMenuConfiguration.QueryType getQueryType() {
        return ActionModeMenuConfiguration.QueryType.SPECIAL;
    }
}
