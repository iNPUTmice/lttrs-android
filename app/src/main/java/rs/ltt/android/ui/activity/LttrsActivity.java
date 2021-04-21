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

package rs.ltt.android.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.work.WorkInfo;

import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import rs.ltt.android.LttrsApplication;
import rs.ltt.android.LttrsNavigationDirections;
import rs.ltt.android.R;
import rs.ltt.android.databinding.ActivityLttrsBinding;
import rs.ltt.android.entity.MailboxOverviewItem;
import rs.ltt.android.entity.MailboxWithRoleAndName;
import rs.ltt.android.ui.ItemAnimators;
import rs.ltt.android.ui.Theme;
import rs.ltt.android.ui.ThreadModifier;
import rs.ltt.android.ui.WeakActionModeCallback;
import rs.ltt.android.ui.adapter.NavigationAdapter;
import rs.ltt.android.ui.model.LttrsViewModel;
import rs.ltt.android.util.Event;
import rs.ltt.android.util.MainThreadExecutor;
import rs.ltt.android.worker.Failure;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.mua.util.KeywordLabel;

public class LttrsActivity extends AppCompatActivity implements ThreadModifier, NavController.OnDestinationChangedListener, MenuItem.OnActionExpandListener, DrawerLayout.DrawerListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LttrsActivity.class);

    public static final String EXTRA_ACCOUNT_ID = "account";

    private static final int NUM_TOOLBAR_ICON = 1;
    private static final List<Integer> MAIN_DESTINATIONS = Arrays.asList(
            R.id.inbox,
            R.id.mailbox,
            R.id.keyword
    );
    private static final List<Integer> QUERY_DESTINATIONS = Arrays.asList(
            R.id.inbox,
            R.id.mailbox,
            R.id.keyword,
            R.id.search
    );
    private static final List<Integer> FULL_SCREEN_DIALOG = Arrays.asList(
            R.id.label_as
    );
    final NavigationAdapter navigationAdapter = new NavigationAdapter();
    private ActivityLttrsBinding binding;
    private LttrsViewModel lttrsViewModel;
    private MenuItem mSearchItem;
    private SearchView mSearchView;
    private ActionMode actionMode;
    private WeakReference<Snackbar> mostRecentSnackbar;

    public static void launch(final AppCompatActivity activity, final long accountId) {
        launch(activity, accountId, false);
    }

    public static void launch(final AppCompatActivity activity, final long accountId, final boolean animate) {
        final Intent intent = new Intent(activity, LttrsActivity.class);
        intent.putExtra(LttrsActivity.EXTRA_ACCOUNT_ID, accountId);
        //the default launch mode of the this activity is set to 'singleTask'
        //to view a new account we want to force recreate the activity
        //the accountId is essentially a final variable and should not be changed during a lifetime
        //of an activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        if (!animate) {
            activity.overridePendingTransition(0, 0);
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGGER.debug("onCreate()");
        binding = DataBindingUtil.setContentView(this, R.layout.activity_lttrs);
        final Intent intent = getIntent();
        final long accountId;
        if (intent != null && intent.hasExtra(EXTRA_ACCOUNT_ID)) {
            accountId = intent.getLongExtra(EXTRA_ACCOUNT_ID, -1);
        } else {
            final long start = SystemClock.elapsedRealtime();
            accountId = LttrsApplication.get(this).getMostRecentlySelectedAccountId();
            LOGGER.warn("Got most recently selected account id from database in {}ms. This should not be happening", (SystemClock.elapsedRealtime() - start));
        }


        final ViewModelProvider viewModelProvider = new ViewModelProvider(
                getViewModelStore(),
                new LttrsViewModel.Factory(
                        getApplication(),
                        accountId
                )
        );
        lttrsViewModel = viewModelProvider.get(LttrsViewModel.class);
        setSupportActionBar(binding.toolbar);

        final NavController navController = getNavController();

        binding.drawerLayout.addDrawerListener(this);

        navigationAdapter.setOnLabelSelectedListener((label, currentlySelected) -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            if (currentlySelected && MAIN_DESTINATIONS.contains(getCurrentDestinationId())) {
                return;
            }
            final boolean navigateToInbox = label.getRole() == Role.INBOX;
            if (navigateToInbox) {
                navController.navigate(LttrsNavigationDirections.actionToInbox());
            } else if (label instanceof MailboxOverviewItem) {
                final MailboxOverviewItem mailbox = (MailboxOverviewItem) label;
                navController.navigate(LttrsNavigationDirections.actionToMailbox(mailbox.id));
            } else if (label instanceof KeywordLabel) {
                final KeywordLabel keyword = (KeywordLabel) label;
                navController.navigate(LttrsNavigationDirections.actionToKeyword(keyword));
            } else {
                throw new IllegalStateException(String.format("%s is an unsupported label", label.getClass()));
            }
            if (mSearchItem != null) {
                mSearchItem.collapseActionView();
            }
            //currently unused should remain here in case we bring scrollable toolbar back
            binding.appBarLayout.setExpanded(true, false);
        });
        navigationAdapter.setOnAccountViewToggledListener(() -> {
            lttrsViewModel.toggleAccountSelectionVisibility();
        });
        navigationAdapter.setOnAccountSelected((id -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            lttrsViewModel.setAccountSelectionVisibility(false);
            if (id != lttrsViewModel.getAccountId()) {
                lttrsViewModel.setSelectedAccount(id);
                launch(this, id);
            }
        }));
        navigationAdapter.setOnAdditionalNavigationItemSelected((type -> {
            switch (type) {
                case ADD_ACCOUNT: {
                    SetupActivity.launch(this);
                }
                break;
                default:
                    throw new IllegalStateException(String.format("Not set up to handle %s", type));
            }
        }));
        binding.navigation.setAdapter(navigationAdapter);
        ItemAnimators.disableChangeAnimation(binding.navigation.getItemAnimator());
        lttrsViewModel.getNavigableItems().observe(this, navigationAdapter::submitList);
        lttrsViewModel.getFailureEvent().observe(this, this::onFailureEvent);
        lttrsViewModel.getSelectedLabel().observe(this, navigationAdapter::setSelectedLabel);
        lttrsViewModel.isAccountSelectionVisible().observe(this, navigationAdapter::setAccountSelectionVisible);
        lttrsViewModel.getAccountName().observe(this, navigationAdapter::setAccountInformation);
        lttrsViewModel.getActivityTitle().observe(this, this::setTitle);
    }

    private void onFailureEvent(Event<Failure> failureEvent) {
        if (failureEvent.isConsumable()) {
            final Failure failure = failureEvent.consume();
            LOGGER.info("processing failure event {}", failure.getException());
            if (failure instanceof Failure.PreExistingMailbox) {
                dismissSnackbar();
                final Failure.PreExistingMailbox preExistingMailbox = (Failure.PreExistingMailbox) failure;
                getNavController().navigate(LttrsNavigationDirections.actionToReassignRole(
                        preExistingMailbox.getMailboxId(),
                        preExistingMailbox.getRole().toString()
                ));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        final int currentDestination = getCurrentDestinationId();
        final boolean showSearch = QUERY_DESTINATIONS.contains(currentDestination);

        getMenuInflater().inflate(R.menu.activity_main, menu);

        mSearchItem = menu.findItem(R.id.action_search);

        mSearchItem.setVisible(showSearch);

        if (showSearch) {
            mSearchView = (SearchView) mSearchItem.getActionView();
            final SearchManager searchManager = getSystemService(SearchManager.class);
            if (searchManager != null) {
                mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            }
            if (currentDestination == R.id.search) {
                prepareToolbarForSearch();
                mSearchItem.expandActionView();
                mSearchView.setQuery(lttrsViewModel.getCurrentSearchTerm(), false);
                mSearchView.clearFocus();
            } else {
                resetToolbar();
            }
            mSearchItem.setOnActionExpandListener(this);
        } else {
            mSearchItem = null;
            mSearchView = null;
            resetToolbar();
        }

        return super.onCreateOptionsMenu(menu);
    }

    public NavController getNavController() {
        return Navigation.findNavController(this, R.id.nav_host_fragment);
    }

    private int getCurrentDestinationId() {
        final NavDestination currentDestination = getNavController().getCurrentDestination();
        return currentDestination == null ? 0 : currentDestination.getId();
    }

    @Override
    public void onStart() {
        getNavController().addOnDestinationChangedListener(this);
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        getNavController().removeOnDestinationChangedListener(this);
    }

    private void configureActionBarForDestination(NavDestination destination) {
        final ActionBar actionbar = getSupportActionBar();
        if (actionbar == null) {
            return;
        }
        final int destinationId = destination.getId();
        actionbar.setDisplayHomeAsUpEnabled(true);
        @DrawableRes final int upIndicator;
        if (MAIN_DESTINATIONS.contains(destinationId)) {
            upIndicator = R.drawable.ic_menu_black_24dp;
        } else if (FULL_SCREEN_DIALOG.contains(destinationId)) {
            upIndicator = R.drawable.ic_baseline_close_24;
        } else {
            upIndicator = R.drawable.ic_arrow_back_white_24dp;
        }
        actionbar.setHomeAsUpIndicator(upIndicator);
        actionbar.setDisplayShowTitleEnabled(destinationId != R.id.thread);
        invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                final NavDestination currentDestination = getNavController().getCurrentDestination();
                if (currentDestination != null && MAIN_DESTINATIONS.contains(currentDestination.getId())) {
                    binding.drawerLayout.openDrawer(GravityCompat.START);
                    return true;
                } else {
                    onBackPressed();
                }
        }
        return super.onOptionsItemSelected(item);

    }

    public void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        final String action = intent == null ? null : intent.getAction();
        if (Intent.ACTION_SEARCH.equals(action)) {
            final String query = Strings.nullToEmpty(intent.getStringExtra(SearchManager.QUERY));
            if (mSearchView != null) {
                mSearchView.setQuery(query, false);
                //this does not work on all phones / android versions; therefor we have this followed by a requestFocus() on the list
                mSearchView.clearFocus();
            }
            binding.navigation.requestFocus();

            lttrsViewModel.insertSearchSuggestion(query);
            getNavController().navigate(LttrsNavigationDirections.actionSearch(query));
        }

    }

    private void showSnackbar(final Snackbar snackbar) {
        this.mostRecentSnackbar = new WeakReference<>(snackbar);
        snackbar.show();
    }

    private void dismissSnackbar() {
        final Snackbar snackbar = this.mostRecentSnackbar != null ? this.mostRecentSnackbar.get() : null;
        if (snackbar != null && snackbar.isShown()) {
            LOGGER.info("Dismissing snackbar");
            snackbar.dismiss();
        }
    }


    @Override
    public void archive(Collection<String> threadIds) {
        final int count = threadIds.size();
        lttrsViewModel.archive(threadIds);
        final Snackbar snackbar = Snackbar.make(
                this.binding.getRoot(),
                getResources().getQuantityString(R.plurals.n_archived, count, count),
                Snackbar.LENGTH_LONG
        );
        snackbar.setAction(R.string.undo, v -> lttrsViewModel.moveToInbox(threadIds));
        showSnackbar(snackbar);
    }

    @Override
    public void moveToInbox(final Collection<String> threadIds) {
        final int count = threadIds.size();
        final Snackbar snackbar = Snackbar.make(
                binding.getRoot(),
                getResources().getQuantityString(R.plurals.n_moved_to_inbox, count, count),
                Snackbar.LENGTH_LONG
        );
        snackbar.setAction(R.string.undo, v -> lttrsViewModel.archive(threadIds));
        showSnackbar(snackbar);
        lttrsViewModel.moveToInbox(threadIds);
    }

    @Override
    public void moveToTrash(final Collection<String> threadIds) {
        final int count = threadIds.size();
        final Snackbar snackbar = Snackbar.make(
                binding.getRoot(),
                getResources().getQuantityString(R.plurals.n_deleted, count, count),
                Snackbar.LENGTH_LONG
        );
        final ListenableFuture<LiveData<WorkInfo>> future = lttrsViewModel.moveToTrash(threadIds);
        snackbar.setAction(R.string.undo, v -> {
            try {
                final LiveData<WorkInfo> workInfoLiveData = future.get();
                final WorkInfo workInfo = workInfoLiveData.getValue();
                lttrsViewModel.cancelMoveToTrash(workInfo, threadIds);
            } catch (Exception e) {
                LOGGER.warn("Unable to cancel moveToTrash operation", e);
            }
        });
        showSnackbar(snackbar);
        future.addListener(() -> {
            try {
                future.get().observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished() && snackbar.isShown()) {
                        LOGGER.info(
                                "Dismissing Move To Trash undo snackbar prematurely because WorkInfo went into state {}",
                                workInfo.getState()
                        );
                        snackbar.dismiss();
                    }
                });
            } catch (Exception e) {
                LOGGER.warn("Unable to observe moveToTrash operation", e);
            }
        }, MainThreadExecutor.getInstance());
    }

    @Override
    public void removeFromMailbox(Collection<String> threadIds, MailboxWithRoleAndName mailbox) {
        final int count = threadIds.size();
        final Snackbar snackbar = Snackbar.make(
                binding.getRoot(),
                getResources().getQuantityString(R.plurals.n_removed_from_x, count, count, mailbox.name),
                Snackbar.LENGTH_LONG
        );
        snackbar.setAction(R.string.undo, v -> lttrsViewModel.copyToMailbox(threadIds, mailbox));
        showSnackbar(snackbar);
        lttrsViewModel.removeFromMailbox(threadIds, mailbox);
    }

    @Override
    public void markRead(Collection<String> threadIds) {
        this.lttrsViewModel.markRead(threadIds);
    }

    @Override
    public void markUnread(Collection<String> threadIds) {
        this.lttrsViewModel.markUnread(threadIds);
    }

    @Override
    public void markImportant(Collection<String> threadIds) {
        this.lttrsViewModel.markImportant(threadIds);
    }

    @Override
    public void markNotImportant(Collection<String> threadIds) {
        this.lttrsViewModel.markNotImportant(threadIds);
    }

    @Override
    public void toggleFlagged(String threadId, boolean target) {
        this.lttrsViewModel.toggleFlagged(threadId, target);
    }

    @Override
    public void addFlag(Collection<String> threadIds) {
        this.lttrsViewModel.addFlag(threadIds);
    }

    @Override
    public void removeFlag(Collection<String> threadIds) {
        this.lttrsViewModel.removeFlag(threadIds);
    }

    @Override
    public void removeFromKeyword(Collection<String> threadIds, final String keyword) {
        final int count = threadIds.size();
        final Snackbar snackbar = Snackbar.make(
                binding.getRoot(),
                getResources().getQuantityString(
                        R.plurals.n_removed_from_x,
                        count,
                        count,
                        KeywordLabel.of(keyword).getName()
                ),
                Snackbar.LENGTH_LONG
        );
        snackbar.setAction(R.string.undo, v -> lttrsViewModel.addKeyword(threadIds, keyword));
        showSnackbar(snackbar);
        lttrsViewModel.removeKeyword(threadIds, keyword);
    }

    @Override
    public void onBackPressed() {
        if (binding != null && binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            if (Boolean.TRUE.equals(lttrsViewModel.isAccountSelectionVisible().getValue())) {
                lttrsViewModel.setAccountSelectionVisibility(false);
                return;
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
        LOGGER.debug("onDestinationChanged({})", destination.getLabel());
        if (!QUERY_DESTINATIONS.contains(destination.getId())) {
            endActionMode();
        }
        configureActionBarForDestination(destination);
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        animateShowSearchToolbar();
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        if (item.isActionViewExpanded()) {
            animateCloseSearchToolbar();
        }
        if (getCurrentDestinationId() == R.id.search) {
            getNavController().navigateUp();
        }
        return true;
    }

    public void animateShowSearchToolbar() {
        prepareToolbarForSearch();
        final int toolbarIconWidth = getResources().getDimensionPixelSize(R.dimen.toolbar_icon_width);
        final int width = binding.toolbar.getWidth() - ((toolbarIconWidth * NUM_TOOLBAR_ICON) / 2);
        Animator createCircularReveal = ViewAnimationUtils.createCircularReveal(binding.toolbar, Theme.isRtl(this) ? binding.toolbar.getWidth() - width : width, binding.toolbar.getHeight() / 2, 0.0f, (float) width);
        createCircularReveal.setDuration(250);
        createCircularReveal.start();
    }

    public void animateCloseSearchToolbar() {
        final int toolbarIconWidth = getResources().getDimensionPixelSize(R.dimen.toolbar_icon_width);
        final int width = binding.toolbar.getWidth() - ((toolbarIconWidth * NUM_TOOLBAR_ICON) / 2);
        Animator createCircularReveal = ViewAnimationUtils.createCircularReveal(binding.toolbar, Theme.isRtl(this) ? binding.toolbar.getWidth() - width : width, binding.toolbar.getHeight() / 2, (float) width, 0.0f);
        createCircularReveal.setDuration(250);
        createCircularReveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                resetToolbar();
            }
        });
        createCircularReveal.start();
    }

    private void resetToolbar() {
        setDisplayShowTitleEnable(true);
        binding.toolbar.setBackgroundColor(Theme.getColor(LttrsActivity.this, R.attr.colorPrimary));
        binding.drawerLayout.setStatusBarBackgroundColor(Theme.getColor(LttrsActivity.this, R.attr.colorPrimaryDark));
    }

    private void prepareToolbarForSearch() {
        setDisplayShowTitleEnable(false);
        binding.toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSurface));
        binding.drawerLayout.setStatusBarBackgroundColor(ContextCompat.getColor(this, R.color.colorStatusBarSearch));
    }


    private void setDisplayShowTitleEnable(final boolean enabled) {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            throw new IllegalStateException("SupportActionBar has not been set");
        }
        actionBar.setDisplayShowTitleEnabled(enabled);
    }

    public ActionMode beginActionMode(final ActionMode.Callback callback) {
        this.actionMode = startSupportActionMode(new WeakActionModeCallback(callback));
        return this.actionMode;
    }

    public void endActionMode() {
        if (this.actionMode != null) {
            this.actionMode.finish();
        }
        this.actionMode = null;
    }

    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
        endActionMode();
    }

    @Override
    public void onDrawerOpened(@NonNull View drawerView) {

    }

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {
        lttrsViewModel.setAccountSelectionVisibility(false);
    }

    @Override
    public void onDrawerStateChanged(int newState) {

    }
}
