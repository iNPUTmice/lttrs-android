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

package rs.ltt.android.repository;

import android.annotation.SuppressLint;
import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkContinuation;
import androidx.work.WorkManager;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.MuaPool;
import rs.ltt.android.cache.LocalAttachment;
import rs.ltt.android.entity.EmailWithReferences;
import rs.ltt.android.entity.IdentityWithNameAndEmail;
import rs.ltt.android.ui.model.ComposeViewModel;
import rs.ltt.android.util.FuturesLiveData;
import rs.ltt.android.worker.AbstractCreateEmailWorker;
import rs.ltt.android.worker.AbstractMuaWorker;
import rs.ltt.android.worker.AttachmentInputMerger;
import rs.ltt.android.worker.BlobUploadWorker;
import rs.ltt.android.worker.DiscardDraftWorker;
import rs.ltt.android.worker.SaveDraftWorker;
import rs.ltt.android.worker.SendEmailWorker;
import rs.ltt.android.worker.SubmitEmailWorker;
import rs.ltt.autocrypt.client.Decision;
import rs.ltt.autocrypt.client.Recommendation;
import rs.ltt.autocrypt.jmap.AutocryptPlugin;
import rs.ltt.jmap.client.blob.MaxUploadSizeExceededException;
import rs.ltt.jmap.client.session.Session;
import rs.ltt.jmap.common.entity.Attachment;
import rs.ltt.jmap.common.entity.EmailAddress;
import rs.ltt.jmap.common.entity.IdentifiableIdentity;
import rs.ltt.jmap.common.entity.Keyword;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.common.entity.capability.CoreCapability;
import rs.ltt.jmap.mua.Mua;
import rs.ltt.jmap.mua.Status;

public class ComposeRepository extends AbstractRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComposeRepository.class);

    private static final ListeningExecutorService ATTACHMENT_EXECUTOR =
            MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    public ComposeRepository(final Application application, final long accountId) {
        super(application, accountId);
        final ListenableFuture<Status> identityRefresh =
                Futures.transformAsync(
                        MuaPool.getInstance(application, accountId),
                        Mua::refreshIdentities,
                        MoreExecutors.directExecutor());
        Futures.addCallback(
                identityRefresh,
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(final Status status) {
                        LOGGER.debug("Identities refresh {}", status);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable throwable) {
                        LOGGER.warn("Unable to refresh identities", throwable);
                    }
                },
                MoreExecutors.directExecutor());
    }

    public ListenableFuture<LocalAttachment> addAttachment(Uri uri) {
        final ListenableFuture<Mua> muaFuture = MuaPool.getInstance(application, accountId);
        final ListenableFuture<Session> sessionFuture =
                Futures.transformAsync(
                        muaFuture,
                        mua -> mua.getJmapClient().getSession(),
                        MoreExecutors.directExecutor());
        return Futures.transform(
                sessionFuture, session -> addAttachment(uri, session), ATTACHMENT_EXECUTOR);
    }

    public static ListenableFuture<List<LocalAttachment>> cacheAttachments(
            final Application application, final Collection<Uri> attachments) {
        return Futures.submit(
                () -> {
                    final ImmutableList.Builder<LocalAttachment> builder =
                            new ImmutableList.Builder<>();
                    for (final Uri uri : attachments) {
                        LocalAttachment.checkContentUri(uri);
                        final LocalAttachment attachment = LocalAttachment.of(application, uri);
                        LocalAttachment.cache(application, uri, attachment);
                        builder.add(attachment);
                    }
                    return builder.build();
                },
                ATTACHMENT_EXECUTOR);
    }

    private LocalAttachment addAttachment(final Uri uri, final Session session) {
        final LocalAttachment attachment = LocalAttachment.of(application, uri);
        checkUploadLimit(session, attachment);
        LocalAttachment.cache(application, uri, attachment);
        return attachment;
    }

    private static void checkUploadLimit(final Session session, final Attachment attachment)
            throws MaxUploadSizeExceededException {
        final CoreCapability coreCapability = session.getCapability(CoreCapability.class);
        final Long maxUploadSize =
                coreCapability == null ? null : coreCapability.getMaxSizeUpload();
        if (maxUploadSize != null && attachment.getSize() > maxUploadSize) {
            throw new MaxUploadSizeExceededException(attachment.getSize(), maxUploadSize);
        }
    }

    public static void deleteLocalAttachment(
            Application application, final LocalAttachment attachment) {
        ATTACHMENT_EXECUTOR.execute(() -> LocalAttachment.delete(application, attachment));
    }

    public static void deleteLocalAttachments(
            final Application application, final List<LocalAttachment> attachments) {
        if (attachments == null) {
            return;
        }
        for (final LocalAttachment attachment : attachments) {
            deleteLocalAttachment(application, attachment);
        }
    }

    public LiveData<Decision> getAutocryptDecision(
            final List<EmailAddress> addresses, final boolean isReplyToEncrypted) {
        final ListenableFuture<Mua> muaFuture = MuaPool.getInstance(application, accountId);
        final ListenableFuture<List<Recommendation>> recommendationFuture =
                Futures.transformAsync(
                        muaFuture,
                        mua ->
                                mua.getPlugin(AutocryptPlugin.class)
                                        .getAutocryptClient()
                                        .getRecommendationsForAddresses(
                                                addresses, isReplyToEncrypted),
                        MoreExecutors.directExecutor());
        return FuturesLiveData.of(
                Futures.transform(
                        recommendationFuture,
                        Recommendation::combine,
                        MoreExecutors.directExecutor()));
    }

    public LiveData<List<IdentityWithNameAndEmail>> getIdentities() {
        return database.identityDao().getIdentitiesLiveData(accountId);
    }

    public ListenableFuture<EmailWithReferences> getEmailWithReferences(final String id) {
        final ListenableFuture<List<String>> addressesFuture =
                database.identityDao().getEmailAddresses();
        final ListenableFuture<EmailWithReferences> emailFuture =
                database.threadAndEmailDao().getEmailWithReferences(accountId, id);
        return Futures.whenAllComplete(addressesFuture, emailFuture)
                .call(
                        () -> {
                            final EmailWithReferences email = emailFuture.get();
                            email.identityEmailAddresses = addressesFuture.get();
                            return email;
                        },
                        MoreExecutors.directExecutor());
    }

    private List<OneTimeWorkRequest> blobUploads(final List<LocalAttachment> localAttachments) {
        final ImmutableList.Builder<OneTimeWorkRequest> uploadWorker =
                new ImmutableList.Builder<>();
        for (final LocalAttachment attachment : localAttachments) {
            uploadWorker.add(
                    new OneTimeWorkRequest.Builder(BlobUploadWorker.class)
                            .setConstraints(CONNECTED_CONSTRAINT)
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .setInputData(BlobUploadWorker.data(accountId, attachment))
                            .build());
        }
        return uploadWorker.build();
    }

    public UUID sendEmail(
            IdentifiableIdentity identity,
            ComposeViewModel.Draft draft,
            final Collection<String> inReplyTo,
            final boolean encrypted,
            EmailWithReferences discard) {
        final EmailCreation emailCreation =
                createEmailWorkRequest(
                        identity, draft, inReplyTo, encrypted, SendEmailWorker.class);
        if (discard != null) {
            final OneTimeWorkRequest discardPreviousDraft =
                    new OneTimeWorkRequest.Builder(DiscardDraftWorker.class)
                            .setConstraints(CONNECTED_CONSTRAINT)
                            .setInputData(DiscardDraftWorker.data(accountId, discard.id))
                            .build();
            emailCreation.workContinuation.then(discardPreviousDraft).enqueue();
        } else {
            emailCreation.workContinuation.enqueue();
        }
        return emailCreation.workRequest.getId();
    }

    public UUID submitEmail(IdentityWithNameAndEmail identity, EmailWithReferences editableEmail) {
        final OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(SubmitEmailWorker.class)
                        .setConstraints(CONNECTED_CONSTRAINT)
                        .setInputData(
                                SubmitEmailWorker.data(
                                        accountId, identity.getId(), editableEmail.id))
                        .build();
        final WorkManager workManager = WorkManager.getInstance(application);
        workManager.enqueue(workRequest);
        return workRequest.getId();
    }

    @SuppressLint("EnqueueWork")
    private EmailCreation createEmailWorkRequest(
            IdentifiableIdentity identity,
            ComposeViewModel.Draft draft,
            final Collection<String> inReplyTo,
            final boolean encrypted,
            final Class<? extends AbstractCreateEmailWorker> clazz) {
        final WorkManager workManager = WorkManager.getInstance(application);
        final Attachments attachments = Attachments.collect(draft, encrypted);
        final OneTimeWorkRequest createEmailWorkRequest =
                new OneTimeWorkRequest.Builder(clazz)
                        .setConstraints(CONNECTED_CONSTRAINT)
                        .setInputMerger(AttachmentInputMerger.class)
                        .setInputData(
                                AbstractCreateEmailWorker.data(
                                        accountId,
                                        identity.getId(),
                                        inReplyTo,
                                        draft.getTo(),
                                        draft.getCc(),
                                        draft.getSubject(),
                                        draft.getBody(),
                                        attachments.processDirectly,
                                        encrypted))
                        .build();
        final WorkContinuation workContinuation;
        if (attachments.requireUploading.isEmpty()) {
            workContinuation =
                    workManager.beginUniqueWork(
                            AbstractMuaWorker.uniqueName(accountId),
                            ExistingWorkPolicy.APPEND_OR_REPLACE,
                            createEmailWorkRequest);
        } else {
            workContinuation =
                    workManager
                            .beginUniqueWork(
                                    AbstractMuaWorker.uniqueName(accountId),
                                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                                    blobUploads(attachments.requireUploading))
                            .then(createEmailWorkRequest);
        }
        return new EmailCreation(createEmailWorkRequest, workContinuation);
    }

    private static class EmailCreation {
        private final OneTimeWorkRequest workRequest;
        private final WorkContinuation workContinuation;

        private EmailCreation(OneTimeWorkRequest workRequest, WorkContinuation workContinuation) {
            this.workRequest = workRequest;
            this.workContinuation = workContinuation;
        }
    }

    public UUID saveDraft(
            final IdentifiableIdentity identity,
            final ComposeViewModel.Draft draft,
            final Collection<String> inReplyTo,
            final boolean encrypted,
            final EmailWithReferences discard) {
        final EmailCreation workRequest =
                createEmailWorkRequest(
                        identity, draft, inReplyTo, encrypted, SaveDraftWorker.class);
        if (discard != null) {
            final OneTimeWorkRequest discardPreviousDraft =
                    new OneTimeWorkRequest.Builder(DiscardDraftWorker.class)
                            .setConstraints(CONNECTED_CONSTRAINT)
                            .setInputData(DiscardDraftWorker.data(accountId, discard.id))
                            .build();
            workRequest.workContinuation.then(discardPreviousDraft).enqueue();
        } else {
            workRequest.workContinuation.enqueue();
        }
        return workRequest.workRequest.getId();
    }

    public boolean discard(EmailWithReferences editableEmail) {
        final OneTimeWorkRequest discardDraft =
                new OneTimeWorkRequest.Builder(DiscardDraftWorker.class)
                        .setConstraints(CONNECTED_CONSTRAINT)
                        .setInputData(DiscardDraftWorker.data(accountId, editableEmail.id))
                        .build();
        final WorkManager workManager = WorkManager.getInstance(application);
        final boolean isOnlyEmailInThread = editableEmail.isOnlyEmailInThread();
        if (isOnlyEmailInThread) {
            insertQueryItemOverwrite(editableEmail.threadId);
        }
        workManager.enqueue(discardDraft);
        return isOnlyEmailInThread;
    }

    private void insertQueryItemOverwrite(final String threadId) {
        IO_EXECUTOR.execute(
                () -> {
                    insertQueryItemOverwrite(threadId, Role.DRAFTS);
                    insertQueryItemOverwrite(threadId, Keyword.DRAFT);
                });
    }

    private static class Attachments {
        private final List<LocalAttachment> requireUploading;
        private final List<Attachment> processDirectly;

        private Attachments(
                final List<LocalAttachment> requireUploading,
                final List<Attachment> processDirectly) {
            this.requireUploading = requireUploading;
            this.processDirectly = processDirectly;
        }

        public static Attachments collect(
                final ComposeViewModel.Draft draft, final boolean encrypted) {
            if (encrypted) {
                return new Attachments(
                        Collections.emptyList(), ImmutableList.copyOf(draft.getAttachments()));
            }
            final ImmutableList.Builder<LocalAttachment> localAttachmentBuilder =
                    new ImmutableList.Builder<>();
            final ImmutableList.Builder<Attachment> attachmentBuilder =
                    new ImmutableList.Builder<>();
            for (final Attachment attachment : draft.getAttachments()) {
                if (attachment instanceof LocalAttachment) {
                    localAttachmentBuilder.add((LocalAttachment) attachment);
                } else {
                    attachmentBuilder.add(attachment);
                }
            }
            return new Attachments(localAttachmentBuilder.build(), attachmentBuilder.build());
        }
    }
}
