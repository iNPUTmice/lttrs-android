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
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

import rs.ltt.android.MuaPool;
import rs.ltt.android.R;
import rs.ltt.android.database.AppDatabase;
import rs.ltt.android.entity.EmailWithReferences;
import rs.ltt.android.entity.IdentifiableWithOwner;
import rs.ltt.android.entity.IdentityWithNameAndEmail;
import rs.ltt.android.repository.ComposeRepository;
import rs.ltt.android.ui.ComposeAction;
import rs.ltt.android.util.Event;
import rs.ltt.android.util.FileSizes;
import rs.ltt.android.util.MergedListsLiveData;
import rs.ltt.android.worker.BlobUploadWorker;
import rs.ltt.android.worker.Failure;
import rs.ltt.jmap.common.entity.Attachment;
import rs.ltt.jmap.common.entity.EmailAddress;
import rs.ltt.jmap.mua.util.AttachmentUtil.CombinedAttachmentSizeExceedsLimitException;
import rs.ltt.jmap.mua.util.EmailAddressUtil;
import rs.ltt.jmap.mua.util.EmailUtil;
import rs.ltt.jmap.mua.util.MailToUri;

public class ComposeViewModel extends AbstractAttachmentViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComposeViewModel.class);

    private final ComposeAction composeAction;
    private final MailToUri uri;
    private final ListenableFuture<EmailWithReferences> email;

    private final MutableLiveData<Event<String>> errorMessage = new MutableLiveData<>();

    private final MutableLiveData<Integer> selectedIdentityPosition = new MutableLiveData<>();
    private final MutableLiveData<Boolean> extendedAddresses = new MutableLiveData<>();
    private final MutableLiveData<String> to = new MutableLiveData<>();
    private final MutableLiveData<String> cc = new MutableLiveData<>();
    private final MutableLiveData<String> subject = new MutableLiveData<>();
    private final MutableLiveData<String> body = new MutableLiveData<>();
    private final MediatorLiveData<List<? extends Attachment>> attachments = new MediatorLiveData<>();
    private final LiveData<List<IdentityWithNameAndEmail>> identities;

    private boolean draftHasBeenHandled = false;

    private ComposeViewModel(@NonNull final Application application, final Parameter parameter) {
        super(application);
        this.composeAction = parameter.composeAction;
        this.uri = parameter.uri;
        //TODO accountIds needs to be a mutable LiveData property that can be changed as soon as we have one attachment
        //in case ComposeAction.NEW it starts will a list of all accountIds or else it starts with a a singleton list
        final LiveData<List<Long>> accountIds = AppDatabase.getInstance(application).accountDao().getAccountIds();
        if (composeAction == ComposeAction.NEW) {
            Preconditions.checkState(
                    parameter.accountId == null,
                    "Account ID should be null when invoking with ComposeAction.NEW"
            );
            this.email = null;
            this.identities = Transformations.switchMap(
                    accountIds,
                    ids -> new MergedListsLiveData<>(ids.stream()
                            .map(id -> getRepository(id).getIdentities())
                            .collect(Collectors.toList())
                    )
            );
            initializeWithEmail(null);
        } else {
            Preconditions.checkNotNull(parameter.emailId);
            Preconditions.checkNotNull(parameter.accountId);
            this.identities = getRepository(parameter.accountId).getIdentities();
            this.email = getRepository(parameter.accountId).getEmailWithReferences(parameter.emailId);
            if (parameter.freshStart) {
                initializeWithEmail();
            }
        }
    }

    private static Collection<String> inReplyTo(@Nullable EmailWithReferences editableEmail, ComposeAction action) {
        if (editableEmail == null) {
            return Collections.emptyList();
        }
        if (action == ComposeAction.EDIT_DRAFT) {
            return editableEmail.inReplyTo;
        }
        if (action == ComposeAction.REPLY_ALL) {
            return editableEmail.messageId;
        }
        return Collections.emptyList();
    }

    private static <T> List<T> nullToEmpty(final List<T> in) {
        return in == null ? Collections.emptyList() : in;
    }

    private ComposeRepository getRepository(Long accountId) {
        return new ComposeRepository(getApplication(), accountId);
    }

    public LiveData<Event<String>> getErrorMessage() {
        return this.errorMessage;
    }

    public MutableLiveData<String> getTo() {
        return this.to;
    }

    public MutableLiveData<String> getCc() {
        return this.cc;
    }

    public LiveData<Boolean> getExtendedAddresses() {
        return this.extendedAddresses;
    }

    public MutableLiveData<String> getSubject() {
        return this.subject;
    }

    public MutableLiveData<String> getBody() {
        return this.body;
    }

    public LiveData<List<? extends Attachment>> getAttachments() {
        return this.attachments;
    }

    public LiveData<List<IdentityWithNameAndEmail>> getIdentities() {
        return this.identities;
    }

    public MutableLiveData<Integer> getSelectedIdentityPosition() {
        return this.selectedIdentityPosition;
    }

    public void showExtendedAddresses() {
        this.extendedAddresses.postValue(true);
    }

    public void suggestHideExtendedAddresses() {
        final String cc = Strings.nullToEmpty(this.cc.getValue());
        if (cc.isEmpty()) {
            this.extendedAddresses.postValue(false);
        }
    }

    public boolean discard() {
        final EmailWithReferences email = getEmail();
        final boolean isOnlyEmailInThread = email == null || getRepository(email.accountId).discard(email);
        this.draftHasBeenHandled = true;
        return isOnlyEmailInThread;
    }

    public UUID send() {
        final IdentityWithNameAndEmail identity = getIdentity();
        if (identity == null) {
            postErrorMessage(R.string.select_sender);
            throw new IllegalStateException();
        }
        final EmailWithReferences editableEmail = getEmail();
        if (editableEmail != null) {
            IdentifiableWithOwner.checkSameOwner(editableEmail, identity);
        }
        final Draft currentDraft = getCurrentDraft();
        if (currentDraft.to.size() <= 0) {
            postErrorMessage(R.string.add_at_least_one_recipient);
            throw new IllegalStateException();
        }
        for (EmailAddress emailAddress : currentDraft.to) {
            if (EmailAddressUtil.isValid(emailAddress)) {
                continue;
            }
            postErrorMessage(R.string.the_address_x_is_invalid, emailAddress.getEmail());
            throw new IllegalStateException();
        }
        LOGGER.info("sending with identity {}", identity.getId());
        final UUID workInfoId;
        final ComposeRepository repository = getRepository(identity.accountId);
        if (this.composeAction == ComposeAction.EDIT_DRAFT
                && editableEmail != null
                && currentDraft.unedited(Draft.edit(editableEmail))) {
            LOGGER.info("draft remains unedited. submitting...");
            workInfoId = repository.submitEmail(identity, editableEmail);
        } else {
            final Collection<String> inReplyTo = inReplyTo(editableEmail, composeAction);
            workInfoId = repository.sendEmail(identity, currentDraft, inReplyTo, editableEmail);
        }
        this.draftHasBeenHandled = true;
        return workInfoId;
    }

    public UUID saveDraft() {
        if (this.draftHasBeenHandled) {
            LOGGER.info("Not storing as draft. Email has already been handled.");
            return null;
        }
        final IdentityWithNameAndEmail identity = getIdentity();
        if (identity == null) {
            LOGGER.info("Not storing draft. No identity has been selected");
            return null;
        }
        final Draft currentDraft = getCurrentDraft();
        if (currentDraft.isEmpty()) {
            LOGGER.info("not storing draft. To, subject, body and attachments are empty.");
            return null;
        }
        final EmailWithReferences editableEmail = getEmail();
        final Draft originalDraft = Draft.with(this.composeAction, this.uri, editableEmail);
        if (originalDraft != null && currentDraft.unedited(originalDraft)) {
            LOGGER.info("Not storing draft. Nothing has been changed");
            draftHasBeenHandled = true;
            return null;
        }
        LOGGER.info("Saving draft");
        final EmailWithReferences discard;
        if (this.composeAction == ComposeAction.EDIT_DRAFT) {
            discard = editableEmail;
            LOGGER.info("Requesting to delete previous draft={}", discard == null ? null : discard.id);
        } else {
            discard = null;
        }
        final Collection<String> inReplyTo = inReplyTo(editableEmail, composeAction);
        final UUID uuid = getRepository(identity.accountId).saveDraft(identity, currentDraft, inReplyTo, discard);
        this.draftHasBeenHandled = true;
        return uuid;
    }

    private IdentityWithNameAndEmail getIdentity() {
        final List<IdentityWithNameAndEmail> identities = this.identities.getValue();
        final Integer selectedIdentity = this.selectedIdentityPosition.getValue();
        if (identities != null && selectedIdentity != null && selectedIdentity < identities.size()) {
            return identities.get(selectedIdentity);
        }
        return null;
    }

    private void postErrorMessage(@StringRes final int res, final Object... objects) {
        postErrorMessage(getApplication().getString(res, objects));
    }

    private void postErrorMessage(final String message) {
        this.errorMessage.postValue(new Event<>(message));
    }

    private EmailWithReferences getEmail() {
        if (this.email != null && this.email.isDone()) {
            try {
                return this.email.get();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private void initializeWithEmail() {
        Futures.addCallback(this.email, new FutureCallback<EmailWithReferences>() {
            @Override
            public void onSuccess(@Nullable final EmailWithReferences result) {
                initializeWithEmail(result);
            }

            @Override
            public void onFailure(@NonNull final Throwable throwable) {
                //TODO print warning and exit view?
            }
        }, MoreExecutors.directExecutor());
    }

    private void initializeWithEmail(final EmailWithReferences email) {
        final Draft draft = Draft.with(composeAction, uri, email);
        if (draft == null) {
            return;
        }
        to.postValue(EmailAddressUtil.toHeaderValue(draft.to));
        cc.postValue(EmailAddressUtil.toHeaderValue(draft.cc));
        if (draft.cc.size() > 0) {
            extendedAddresses.postValue(true);
        }
        subject.postValue(draft.subject);
        body.postValue(draft.body);
        attachments.postValue(draft.attachments);
    }

    private Draft getCurrentDraft() {
        return Draft.of(this.to, this.cc, this.subject, this.body, this.attachments);
    }

    public void addAttachment(final Uri uri) {
        final IdentityWithNameAndEmail identity = getIdentity();
        if (identity == null) {
            postErrorMessage(R.string.select_sender);
            return;
        }
        final LiveData<WorkInfo> workInfoLiveData = uploadAttachment(identity, uri);
        //TODO do we want to create StubAttachment while waiting for Worker?
        waitForUpload(workInfoLiveData);
    }

    private void waitForUpload(final LiveData<WorkInfo> workInfoLiveData) {
        attachments.addSource(workInfoLiveData, workInfo -> {
            final WorkInfo.State state = workInfo.getState();
            if (state.isFinished()) {
                attachments.removeSource(workInfoLiveData);
                if (state == WorkInfo.State.SUCCEEDED) {
                    addAttachment(BlobUploadWorker.getAttachment(workInfo));
                } else if (state == WorkInfo.State.FAILED) {
                    postAttachmentFailure(workInfo.getOutputData());
                }
            }
        });
    }

    private void postAttachmentFailure(final Data data) {
        final Failure failure;
        try {
            failure = Failure.of(data);
        } catch (IllegalArgumentException e) {
            postErrorMessage(R.string.failed_to_upload_attachment);
            return;
        }
        if (failure instanceof Failure.MaxUploadSizeExceeded) {
            final long max = ((Failure.MaxUploadSizeExceeded) failure).getMaxUploadSize();
            postErrorMessage(R.string.the_file_exceeds_the_limit_of_x, FileSizes.toString(max));
        } else if (failure.getException() != CancellationException.class) {
            if (Strings.isNullOrEmpty(failure.getMessage())) {
                postErrorMessage(R.string.failed_to_upload_attachment);
            } else {
                postErrorMessage(failure.getMessage());
            }
        }
    }

    private void addAttachment(final Attachment attachment) {
        final ImmutableList.Builder<Attachment> attachmentBuilder = new ImmutableList.Builder<>();
        attachmentBuilder.addAll(nullToEmpty(this.attachments.getValue()));
        attachmentBuilder.add(attachment);
        refreshAttachments(attachmentBuilder.build());
    }

    private LiveData<WorkInfo> uploadAttachment(final IdentityWithNameAndEmail identity, final Uri uri) {
        final WorkManager workManager = WorkManager.getInstance(getApplication());
        final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(BlobUploadWorker.class)
                .setInputData(BlobUploadWorker.data(identity.getAccountId(), uri))
                .build();
        workManager.enqueueUniqueWork(
                BlobUploadWorker.uniqueName(),
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                workRequest
        );
        return workManager.getWorkInfoByIdLiveData(workRequest.getId());
    }

    public void deleteAttachment(final Attachment attachment) {
        final List<? extends Attachment> current = new ArrayList<>(nullToEmpty(this.attachments.getValue()));
        if (current.remove(attachment)) {
            refreshAttachments(ImmutableList.copyOf(current));
        }
    }

    private void refreshAttachments(final List<Attachment> attachments) {
        this.attachments.postValue(attachments);
        final IdentityWithNameAndEmail identity = getIdentity();
        if (identity != null) {
            verifyAttachmentsDoNotExceedLimit(identity.getAccountId(), attachments);
        }
    }

    private void verifyAttachmentsDoNotExceedLimit(final long accountId, final List<Attachment> attachments) {
        final ListenableFuture<Void> verificationFuture = Futures.transformAsync(
                MuaPool.getInstance(getApplication(), accountId),
                mua -> mua.verifyAttachmentsDoNotExceedLimit(attachments),
                MoreExecutors.directExecutor()
        );
        Futures.addCallback(verificationFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void unused) {
                LOGGER.debug("Attachments passed size check");
            }

            @Override
            public void onFailure(@NotNull final Throwable throwable) {
                if (throwable instanceof CombinedAttachmentSizeExceedsLimitException) {
                    CombinedAttachmentSizeExceedsLimitException exception = (CombinedAttachmentSizeExceedsLimitException) throwable;
                    postErrorMessage(R.string.combined_size_of_attachments_exceeds_the_limit_of_x, FileSizes.toString(exception.getLimit()));
                }
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected void queueDownload(final String emailId, final Attachment attachment) {
        if (emailId == null) {
            postErrorMessage(R.string.attachment_is_not_cached);
            return;
        }
        super.queueDownload(emailId, attachment);
    }

    @Override
    protected long getAccountId() {
        final IdentityWithNameAndEmail identity = getIdentity();
        if (identity == null) {
            throw new IllegalStateException("There are attachments but no selected identity");
        }
        return identity.getAccountId();
    }

    public void open(final Attachment attachment) {
        final EmailWithReferences email = getEmail();
        final String emailId = email == null ? null : email.getId();
        open(emailId, attachment);
    }

    public static class Parameter {
        public final Long accountId;
        public final boolean freshStart;
        public final ComposeAction composeAction;
        public final String emailId;
        public final MailToUri uri;

        public Parameter(Long accountId, boolean freshStart, ComposeAction composeAction, String emailId) {
            this.accountId = accountId;
            this.freshStart = freshStart;
            this.composeAction = composeAction;
            this.emailId = emailId;
            this.uri = null;
        }

        public Parameter(MailToUri uri, boolean freshStart) {
            this.accountId = null;
            this.freshStart = freshStart;
            this.composeAction = ComposeAction.NEW;
            this.emailId = null;
            this.uri = uri;
        }
    }

    public static class Draft {
        private final Collection<EmailAddress> to;
        private final Collection<EmailAddress> cc;
        private final String subject;
        private final String body;
        private final List<? extends Attachment> attachments;

        private Draft(Collection<EmailAddress> to, Collection<EmailAddress> cc, String subject, String body, List<? extends Attachment> attachments) {
            this.to = to;
            this.cc = cc;
            this.subject = subject;
            this.body = body;
            this.attachments = attachments;
        }

        public static Draft of(LiveData<String> to, LiveData<String> cc, LiveData<String> subject, LiveData<String> body, LiveData<List<? extends Attachment>> attachments) {
            return new Draft(
                    EmailAddressUtil.parse(Strings.nullToEmpty(to.getValue())),
                    EmailAddressUtil.parse(Strings.nullToEmpty(cc.getValue())),
                    Strings.nullToEmpty(subject.getValue()),
                    Strings.nullToEmpty(body.getValue()),
                    nullToEmpty(attachments.getValue())
            );
        }

        private static Draft newEmail(final MailToUri uri) {
            if (uri == null) {
                return null;
            }
            return new Draft(
                    uri.getTo(),
                    uri.getCc(),
                    Strings.nullToEmpty(uri.getSubject()),
                    Strings.nullToEmpty(uri.getBody()),
                    Collections.emptyList()
            );
        }

        private static Draft edit(EmailWithReferences email) {
            return new Draft(
                    email.getTo(),
                    email.getCc(),
                    email.subject,
                    email.getText(),
                    email.getAttachments()
            );
        }

        private static Draft replyAll(EmailWithReferences email) {
            EmailUtil.ReplyAddresses replyAddresses = EmailUtil.replyAll(email);
            return new Draft(
                    replyAddresses.getTo(),
                    replyAddresses.getCc(),
                    EmailUtil.getResponseSubject(email),
                    "",
                    Collections.emptyList() // TODO replace with email.getAttachments
            );
        }

        public static Draft with(final ComposeAction action, final MailToUri uri, EmailWithReferences editableEmail) {
            switch (action) {
                case NEW:
                    return newEmail(uri);
                case EDIT_DRAFT:
                    return edit(editableEmail);
                case REPLY_ALL:
                    return replyAll(editableEmail);
                default:
                    throw new IllegalStateException(String.format("Unknown action %s", action));
            }
        }

        public boolean isEmpty() {
            return to.isEmpty() && subject.trim().isEmpty() && body.trim().isEmpty() && attachments.isEmpty();
        }

        public Collection<EmailAddress> getTo() {
            return to;
        }

        public Collection<EmailAddress> getCc() {
            return cc;
        }

        public String getSubject() {
            return subject;
        }

        public String getBody() {
            return body;
        }

        public boolean unedited(final Draft draft) {
            return draft != null
                    && EmailAddressUtil.equalCollections(draft.getTo(), to)
                    && EmailAddressUtil.equalCollections(draft.getCc(), cc)
                    && subject.equals(draft.subject)
                    && body.equals(draft.body)
                    && attachments.equals(draft.attachments);
        }

        public Collection<? extends Attachment> getAttachments() {
            return attachments;
        }
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final Application application;
        private final ComposeViewModel.Parameter parameter;

        public Factory(@NonNull Application application, @NonNull ComposeViewModel.Parameter parameter) {
            this.application = application;
            this.parameter = parameter;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return Objects.requireNonNull(modelClass.cast(new ComposeViewModel(application, parameter)));
        }
    }
}
