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
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import rs.ltt.android.entity.ThreadOverviewItem;
import rs.ltt.android.ui.ActionModeMenuConfiguration;
import rs.ltt.android.ui.QueryItemTouchHelper;
import rs.ltt.android.ui.model.AbstractQueryViewModel;
import rs.ltt.android.ui.model.KeywordQueryViewModel;
import rs.ltt.jmap.common.entity.Keyword;
import rs.ltt.jmap.mua.util.KeywordLabel;

public class KeywordQueryFragment extends AbstractQueryFragment {

    private KeywordQueryViewModel keywordQueryViewModel;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        final KeywordLabel keywordLabel =
                KeywordQueryFragmentArgs.fromBundle(bundle == null ? new Bundle() : bundle)
                        .getKeyword();
        final ViewModelProvider viewModelProvider =
                new ViewModelProvider(
                        getViewModelStore(),
                        new KeywordQueryViewModel.Factory(
                                requireActivity().getApplication(),
                                getLttrsViewModel().getAccountId(),
                                keywordLabel.getKeyword()));
        this.keywordQueryViewModel = viewModelProvider.get(KeywordQueryViewModel.class);
        onLabelOpened(keywordLabel);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected AbstractQueryViewModel getQueryViewModel() {
        return keywordQueryViewModel;
    }

    @Override
    protected QueryItemTouchHelper.Swipable onQueryItemSwipe(ThreadOverviewItem item) {
        if (Keyword.DRAFT.equals(keywordQueryViewModel.getKeyword())) {
            return QueryItemTouchHelper.Swipable.NO;
        } else if (Keyword.FLAGGED.equals(keywordQueryViewModel.getKeyword())) {
            return QueryItemTouchHelper.Swipable.REMOVE_FLAGGED;
        } else {
            return QueryItemTouchHelper.Swipable.REMOVE_LABEL;
        }
    }

    @Override
    protected void onQueryItemSwiped(final ThreadOverviewItem item) {
        removeKeyword(item.threadId);
    }

    @Override
    protected boolean isQueryItemRemovedAfterSwipe() {
        return true;
    }

    @Override
    public void onFlaggedToggled(final String threadId, final boolean target) {
        if (Keyword.FLAGGED.equals(keywordQueryViewModel.getKeyword()) && !target) {
            removeKeyword(threadId);
            return;
        }
        getThreadModifier().toggleFlagged(threadId, target);
    }

    private void removeKeyword(final String threadId) {
        removeLabel(ImmutableSet.of(threadId));
    }

    @Override
    protected boolean showComposeButton() {
        return true;
    }

    @Override
    void removeLabel(Collection<String> threadIds) {
        getThreadModifier().removeFromKeyword(threadIds, keywordQueryViewModel.getKeyword());
    }

    @Override
    protected ActionModeMenuConfiguration.QueryType getQueryType() {
        if (Keyword.FLAGGED.equals(keywordQueryViewModel.getKeyword())) {
            return ActionModeMenuConfiguration.QueryType.FLAGGED;
        } else if (Keyword.DRAFT.equals(keywordQueryViewModel.getKeyword())) {
            return ActionModeMenuConfiguration.QueryType.SPECIAL;
        } else {
            return ActionModeMenuConfiguration.QueryType.LABEL;
        }
    }
}
