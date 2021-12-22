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
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkInfo;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.LttrsApplication;
import rs.ltt.android.entity.AccountName;
import rs.ltt.android.repository.LttrsRepository;
import rs.ltt.android.repository.MainRepository;
import rs.ltt.android.ui.AdditionalNavigationItem;
import rs.ltt.android.ui.EmptyMailboxAction;
import rs.ltt.android.util.Event;
import rs.ltt.android.worker.Failure;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRole;
import rs.ltt.jmap.common.entity.Keyword;
import rs.ltt.jmap.mua.util.LabelUtil;
import rs.ltt.jmap.mua.util.LabelWithCount;
import rs.ltt.jmap.mua.util.Navigable;

public class LttrsViewModel extends AndroidViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(LttrsViewModel.class);

    private final LiveData<List<Navigable>> navigableItems;
    private final MutableLiveData<LabelWithCount> selectedLabel = new MutableLiveData<>();
    private final MutableLiveData<Boolean> accountSelectionVisible = new MutableLiveData<>(false);
    private final MutableLiveData<String> activityTitle = new MutableLiveData<>();
    private final MainRepository mainRepository;
    private final long accountId;
    private final LttrsRepository lttrsRepository;
    private String currentSearchTerm;

    public LttrsViewModel(@NonNull Application application, final long accountId) {
        super(application);
        LOGGER.debug("creating instance of LttrsViewModel");
        this.mainRepository = new MainRepository(application);
        this.accountId = accountId;
        this.lttrsRepository = new LttrsRepository(application, accountId);
        final LiveData<List<Navigable>> labels =
                Transformations.map(
                        this.lttrsRepository.getMailboxes(),
                        boxes -> ImmutableList.copyOf(LabelUtil.fillUpAndSort(boxes)));
        final LiveData<List<Navigable>> accounts =
                Transformations.map(
                        this.mainRepository.getAccountNames(),
                        names -> {
                            final ImmutableList.Builder<Navigable> builder =
                                    new ImmutableList.Builder<>();
                            builder.addAll(names);
                            builder.addAll(AdditionalNavigationItem.ACCOUNT_SELECTOR_ITEMS);
                            return builder.build();
                        });
        this.navigableItems =
                Transformations.switchMap(
                        this.accountSelectionVisible,
                        input -> {
                            if (Boolean.TRUE.equals(input)) {
                                return accounts;
                            } else {
                                return labels;
                            }
                        });
    }

    public LiveData<Event<Failure>> getFailureEvent() {
        return this.lttrsRepository.getFailureEvent();
    }

    public long getAccountId() {
        return this.accountId;
    }

    public LiveData<AccountName> getAccountName() {
        return this.mainRepository.getAccountName(this.accountId);
    }

    public String getCurrentSearchTerm() {
        return currentSearchTerm;
    }

    public void setCurrentSearchTerm(String currentSearchTerm) {
        this.currentSearchTerm = currentSearchTerm;
    }

    public void clearActivityTitle() {
        this.activityTitle.postValue(null);
    }

    public LiveData<String> getActivityTitle() {
        return this.activityTitle;
    }

    public void setActivityTitle(final String title) {
        this.activityTitle.postValue(title);
    }

    public void setActivityTitle(final @StringRes int res) {
        this.activityTitle.postValue(getApplication().getString(res));
    }

    public LiveData<LabelWithCount> getSelectedLabel() {
        return this.selectedLabel;
    }

    public void setSelectedLabel(final LabelWithCount label) {
        this.selectedLabel.postValue(label);
    }

    public void toggleAccountSelectionVisibility() {
        final boolean current = Boolean.TRUE.equals(this.accountSelectionVisible.getValue());
        this.accountSelectionVisible.postValue(!current);
    }

    public void setAccountSelectionVisibility(final boolean visibility) {
        this.accountSelectionVisible.postValue(visibility);
    }

    public LiveData<Boolean> isAccountSelectionVisible() {
        return this.accountSelectionVisible;
    }

    public LiveData<List<Navigable>> getNavigableItems() {
        return this.navigableItems;
    }

    public void insertSearchSuggestion(String term) {
        this.mainRepository.insertSearchSuggestion(term);
    }

    public ListenableFuture<LiveData<WorkInfo>> moveToTrash(final Collection<String> threadIds) {
        return this.lttrsRepository.moveToTrash(threadIds);
    }

    public void cancelMoveToTrash(final WorkInfo workInfo, final Collection<String> threadIds) {
        this.lttrsRepository.cancelMoveToTrash(workInfo, threadIds);
    }

    public void archive(Collection<String> threadIds) {
        this.lttrsRepository.archive(threadIds);
    }

    public void moveToInbox(Collection<String> threadIds) {
        this.lttrsRepository.moveToInbox(threadIds);
    }

    public void removeFromMailbox(
            final Collection<String> threadIds, final IdentifiableMailboxWithRole mailbox) {
        this.lttrsRepository.removeFromMailbox(threadIds, mailbox);
    }

    public void copyToMailbox(
            final Collection<String> threadIds, final IdentifiableMailboxWithRole mailbox) {
        this.lttrsRepository.copyToMailbox(threadIds, mailbox);
    }

    public void removeKeyword(final Collection<String> threadIds, final String keyword) {
        this.lttrsRepository.removeKeyword(threadIds, keyword);
    }

    public void addKeyword(final Collection<String> threadIds, final String keyword) {
        this.lttrsRepository.addKeyword(threadIds, keyword);
    }

    public void toggleFlagged(String threadId, boolean target) {
        this.lttrsRepository.toggleFlagged(ImmutableSet.of(threadId), target);
    }

    public void markRead(final Collection<String> threadIds) {
        this.lttrsRepository.markRead(threadIds);
    }

    public void markUnread(final Collection<String> threadIds) {
        this.lttrsRepository.markUnRead(threadIds);
    }

    public void markImportant(final Collection<String> threadIds) {
        this.lttrsRepository.markImportant(threadIds);
    }

    public void markNotImportant(final Collection<String> threadIds) {
        this.lttrsRepository.markNotImportant(threadIds);
    }

    public void addFlag(final Collection<String> threadIds) {
        this.addKeyword(threadIds, Keyword.FLAGGED);
    }

    public void removeFlag(final Collection<String> threadIds) {
        this.removeKeyword(threadIds, Keyword.FLAGGED);
    }

    public void observeForFailure(final UUID id) {
        this.lttrsRepository.observeForFailure(id);
    }

    public void observeForFailure(final List<UUID> ids) {
        this.lttrsRepository.observeForFailure(ids);
    }

    public void setSelectedAccount(long id) {
        this.mainRepository.setSelectedAccount(id);
        LttrsApplication.get(getApplication()).invalidateMostRecentlySelectedAccountId();
    }

    public void executeEmptyMailboxAction(final EmptyMailboxAction action) {
        this.lttrsRepository.executeEmptyMailboxAction(action);
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final Application application;
        private final long accountId;

        public Factory(@NonNull final Application application, @NonNull final long accountId) {
            this.application = application;
            this.accountId = accountId;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return Objects.requireNonNull(
                    modelClass.cast(new LttrsViewModel(application, accountId)));
        }
    }
}
