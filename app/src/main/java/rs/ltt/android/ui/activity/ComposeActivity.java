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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.base.Preconditions;
import com.google.common.net.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import rs.ltt.android.LttrsApplication;
import rs.ltt.android.R;
import rs.ltt.android.databinding.ActivityComposeBinding;
import rs.ltt.android.databinding.ItemAttachmentBinding;
import rs.ltt.android.ui.ChipDrawableSpan;
import rs.ltt.android.ui.ComposeAction;
import rs.ltt.android.ui.model.ComposeViewModel;
import rs.ltt.android.util.MediaTypes;
import rs.ltt.jmap.common.entity.Attachment;
import rs.ltt.jmap.mua.util.MailToUri;

//TODO handle save instance state
public class ComposeActivity extends AppCompatActivity {

    public static final int REQUEST_EDIT_DRAFT = 0x100;
    public static final String EDITING_TASK_ID_EXTRA = "work_request_id";
    public static final String DISCARDED_THREAD_EXTRA = "discarded_thread";
    private static final Logger LOGGER = LoggerFactory.getLogger(ComposeActivity.class);
    private static final String ACCOUNT_EXTRA = "account";
    private static final String COMPOSE_ACTION_EXTRA = "compose_action";
    private static final String EMAIL_ID_EXTRA = "email_id";
    private ActivityComposeBinding binding;
    private ComposeViewModel composeViewModel;
    private ActivityResultLauncher<String> getAttachmentLauncher;

    public static void compose(final Fragment fragment) {
        launch(fragment, null, null, ComposeAction.NEW);
    }

    public static void editDraft(final Fragment fragment, Long account, final String emailId) {
        launch(fragment, account, emailId, ComposeAction.EDIT_DRAFT);
    }

    public static void replyAll(final Fragment fragment, Long account, final String emailId) {
        launch(fragment, account, emailId, ComposeAction.REPLY_ALL);
    }

    private static void launch(final Fragment fragment,
                               final Long account,
                               final String emailId,
                               final ComposeAction action) {
        Preconditions.checkNotNull(action);
        final Intent intent = new Intent(fragment.getContext(), ComposeActivity.class);
        if (account != null) {
            LOGGER.info("launching for account {} with {}", account, action);
            intent.putExtra(ACCOUNT_EXTRA, account);
        } else {
            LOGGER.info("launching with {}", action);
        }
        intent.putExtra(COMPOSE_ACTION_EXTRA, action.toString());
        if (emailId != null) {
            intent.putExtra(EMAIL_ID_EXTRA, emailId);
        }
        fragment.startActivityForResult(intent, REQUEST_EDIT_DRAFT);
    }

    public static void launch(AppCompatActivity activity, final Uri uri) {
        final Intent nextIntent = new Intent(activity, ComposeActivity.class);
        nextIntent.setAction(Intent.ACTION_VIEW);
        nextIntent.setData(uri);
    }

    private static MailToUri getUri(@NonNull final Intent intent) {
        final Uri data = intent.getData();
        if (data == null) {
            return null;
        }
        try {
            return MailToUri.get(data.toString());
        } catch (final IllegalArgumentException e) {
            LOGGER.warn("activity was called with invalid URI {}. {}", data.toString(), e.getMessage());
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (LttrsApplication.get(this).noAccountsConfigured()) {
            redirectToSetupActivity();
            finishAffinity();
            return;
        }

        this.getAttachmentLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::addAttachment
        );

        binding = DataBindingUtil.setContentView(this, R.layout.activity_compose);

        setupActionBar();

        final ViewModelProvider viewModelProvider = new ViewModelProvider(
                this,
                new ComposeViewModel.Factory(
                        getApplication(),
                        getViewModelParameter(savedInstanceState)
                )
        );
        composeViewModel = viewModelProvider.get(ComposeViewModel.class);

        composeViewModel.getErrorMessage().observe(this, event -> {
            if (event.isConsumable()) {
                final String message = event.consume();
                new MaterialAlertDialogBuilder(this)
                        .setTitle(message)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        });

        binding.setComposeViewModel(composeViewModel);
        binding.setLifecycleOwner(this);

        binding.to.addTextChangedListener(new ChipTextWatcher(binding.to));
        binding.to.setOnFocusChangeListener(
                (v, hasFocus) -> ChipDrawableSpan.apply(this, binding.to.getEditableText(), hasFocus)
        );

        binding.cc.addTextChangedListener(new ChipTextWatcher(binding.cc));
        binding.cc.setOnFocusChangeListener(
                (v, hasFocus) -> ChipDrawableSpan.apply(this, binding.cc.getEditableText(), hasFocus)
        );

        binding.moreAddresses.setOnClickListener((v -> composeViewModel.showExtendedAddresses()));

        binding.subject.setOnFocusChangeListener(this::focusOnBodyOrSubject);
        binding.body.setOnFocusChangeListener(this::focusOnBodyOrSubject);

        binding.toLabel.setOnClickListener(v -> requestFocusAndOpenKeyboard(binding.to));
        binding.ccLabel.setOnClickListener(v -> requestFocusAndOpenKeyboard(binding.cc));
        binding.placeholder.setOnClickListener(v -> requestFocusAndOpenKeyboard(binding.body));

        composeViewModel.getAttachments().observe(this, this::onAttachmentsUpdated);

        //TODO once we handle instance state ourselves we need to call ChipDrawableSpan.reset() on `to`
    }

    private void onAttachmentsUpdated(final List<Attachment> attachments) {
        if (attachments.isEmpty()) {
            binding.attachments.setVisibility(View.GONE);
        } else {
            binding.attachments.setVisibility(View.VISIBLE);
            binding.attachments.removeAllViews();
            for (final Attachment attachment : attachments) {
                binding.attachments.addView(renderAttachment(attachment));
            }
        }
    }

    private View renderAttachment(final Attachment attachment) {
        final ItemAttachmentBinding attachmentBinding = ItemAttachmentBinding.inflate(
                getLayoutInflater(),
                binding.attachments,
                false
        );
        attachmentBinding.setAttachment(attachment);
        attachmentBinding.action.setImageResource(R.drawable.ic_baseline_close_24);
        attachmentBinding.action.setOnClickListener((v) -> deleteAttachment(attachment));
        //TODO wire up 'open/view'
        return attachmentBinding.getRoot();
    }

    private void deleteAttachment(final Attachment attachment) {
        composeViewModel.deleteAttachment(attachment);
    }

    private void addAttachment(final Uri uri) {
        if (uri == null) {
            LOGGER.warn("addAttachment called with null uri");
            return;
        }
        composeViewModel.addAttachment(uri);
    }

    private void redirectToSetupActivity() {
        final Intent currentIntent = getIntent();
        final Uri data = currentIntent == null ? null : currentIntent.getData();
        final MailToUri uri = data == null ? null : MailToUri.parse(data.toString());
        final Intent nextIntent = new Intent(this, SetupActivity.class);
        if (uri != null) {
            nextIntent.putExtra(SetupActivity.EXTRA_NEXT_ACTION, data.toString());
        }
        startActivity(nextIntent);
    }

    private ComposeViewModel.Parameter getViewModelParameter(final Bundle savedInstanceState) {
        final boolean freshStart = savedInstanceState == null || savedInstanceState.isEmpty();
        final Intent i = getIntent();
        final MailToUri uri = i != null ? getUri(i) : null;
        if (uri != null) {
            return new ComposeViewModel.Parameter(uri, freshStart);
        }
        final Long account;
        if (i != null && i.hasExtra(ACCOUNT_EXTRA)) {
            account = i.getLongExtra(ACCOUNT_EXTRA, 0L);
        } else {
            account = null;
        }
        final ComposeAction action = ComposeAction.of(i == null ? null : i.getStringExtra(COMPOSE_ACTION_EXTRA));
        final String emailId = i == null ? null : i.getStringExtra(EMAIL_ID_EXTRA);
        return new ComposeViewModel.Parameter(account, freshStart, action, emailId);
    }

    private void focusOnBodyOrSubject(final View view, final boolean hasFocus) {
        if (hasFocus) {
            composeViewModel.suggestHideExtendedAddresses();
        }
    }

    private void requestFocusAndOpenKeyboard(AppCompatEditText editText) {
        editText.requestFocus();
        final InputMethodManager inputMethodManager = getSystemService(InputMethodManager.class);
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.activity_compose, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            saveDraft();
            return super.onOptionsItemSelected(item);
        } else if (itemId == R.id.action_send) {
            send();
            return true;
        } else if (itemId == R.id.action_discard) {
            discardDraft();
            return true;
        } else if (itemId == R.id.action_attach_file) {
            this.getAttachmentLauncher.launch(MediaTypes.toString(MediaType.ANY_TYPE));
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        saveDraft();
        super.onBackPressed();
    }

    private void send() {
        try {
            final UUID workInfoId = composeViewModel.send();
            setResultIntent(workInfoId);
            finish();
        } catch (final IllegalStateException e) {
            LOGGER.debug("Aborting send", e);
        }
    }

    private void saveDraft() {
        final UUID uuid = composeViewModel == null ? null : composeViewModel.saveDraft();
        if (uuid != null) {
            LOGGER.info("Storing draft saving worker task uuid");
            setResultIntent(uuid);
        }
    }

    private void setResultIntent(final UUID workInfoId) {
        final Intent intent = new Intent();
        intent.putExtra(EDITING_TASK_ID_EXTRA, workInfoId);
        setResult(RESULT_OK, intent);
    }

    private void discardDraft() {
        final boolean isOnlyEmailInThread = composeViewModel.discard();
        Intent intent = new Intent();
        intent.putExtra(DISCARDED_THREAD_EXTRA, isOnlyEmailInThread);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onDestroy() {
        if (isFinishing() && composeViewModel != null) {
            composeViewModel.saveDraft();
        }
        super.onDestroy();
    }

    private void setupActionBar() {
        setSupportActionBar(binding.toolbar);
        final ActionBar actionbar = requireActionBar();
        final boolean displayUpButton = !isTaskRoot();
        actionbar.setHomeButtonEnabled(displayUpButton);
        actionbar.setDisplayHomeAsUpEnabled(displayUpButton);
    }

    private @NonNull
    ActionBar requireActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            throw new IllegalStateException("No ActionBar found");
        }
        return actionBar;
    }

    private static class ChipTextWatcher implements TextWatcher {

        private final EditText editText;

        private ChipTextWatcher(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            ChipDrawableSpan.apply(editText.getContext(), editable, editText.hasFocus());
        }
    }
}
