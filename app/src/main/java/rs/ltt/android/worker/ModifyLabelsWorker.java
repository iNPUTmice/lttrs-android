package rs.ltt.android.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.entity.EmailWithMailboxes;
import rs.ltt.android.entity.MailboxWithRoleAndName;
import rs.ltt.android.util.CharSequences;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRoleAndName;
import rs.ltt.jmap.common.entity.Role;

public class ModifyLabelsWorker extends AbstractMailboxModificationWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyLabelsWorker.class);

    private static final String ADD_KEY = "add";
    private static final String REMOVE_KEY = "remove";

    private final List<IdentifiableMailboxWithRoleAndName> add;
    private final List<IdentifiableMailboxWithRoleAndName> remove;

    public ModifyLabelsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = workerParams.getInputData();
        this.add = catchException(() -> of(data.getByteArray(ADD_KEY)));
        this.remove = catchException(() -> of(data.getByteArray(REMOVE_KEY)));
    }

    private static List<IdentifiableMailboxWithRoleAndName> of(final byte[] bytes)
            throws IOException {
        final DataInputStream dataInputStream =
                new DataInputStream(new ByteArrayInputStream(bytes));
        final int count = dataInputStream.readInt();
        if (count == 0) {
            return Collections.emptyList();
        }
        final ImmutableList.Builder<IdentifiableMailboxWithRoleAndName> mailboxesBuilder =
                new ImmutableList.Builder<>();
        for (int i = 0; i < count; ++i) {
            mailboxesBuilder.add(read(dataInputStream));
        }
        return mailboxesBuilder.build();
    }

    private static <T> T catchException(final Callable<T> callable) {
        try {
            return callable.call();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static MailboxWithRoleAndName read(final DataInputStream dataInputStream)
            throws IOException {
        final MailboxWithRoleAndName mailbox = new MailboxWithRoleAndName();
        mailbox.id = Strings.emptyToNull(dataInputStream.readUTF());
        final String role = Strings.emptyToNull(dataInputStream.readUTF());
        mailbox.role = role == null ? null : Role.valueOf(role);
        mailbox.name = Strings.emptyToNull(dataInputStream.readUTF());
        return mailbox;
    }

    private static byte[] of(List<IdentifiableMailboxWithRoleAndName> mailboxes)
            throws IOException {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        dataOutputStream.writeInt(mailboxes.size());
        for (final IdentifiableMailboxWithRoleAndName mailbox : mailboxes) {
            write(dataOutputStream, mailbox);
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static void write(
            final DataOutputStream dataOutputStream,
            final IdentifiableMailboxWithRoleAndName mailbox)
            throws IOException {
        dataOutputStream.writeUTF(Strings.nullToEmpty(mailbox.getId()));
        dataOutputStream.writeUTF(
                mailbox.getRole() == null
                        ? CharSequences.EMPTY_STRING
                        : mailbox.getRole().toString());
        dataOutputStream.writeUTF(Strings.nullToEmpty(mailbox.getName()));
    }

    public static Data data(
            Long account,
            String threadId,
            List<IdentifiableMailboxWithRoleAndName> add,
            List<IdentifiableMailboxWithRoleAndName> remove) {
        return new Data.Builder()
                .putLong(ACCOUNT_KEY, account)
                .putString(THREAD_ID_KEY, threadId)
                .putByteArray(ADD_KEY, catchException(() -> of(add)))
                .putByteArray(REMOVE_KEY, catchException(() -> of(remove)))
                .build();
    }

    @Override
    protected ListenableFuture<Boolean> modify(final List<EmailWithMailboxes> emails)
            throws ExecutionException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "Add {} and remove {} from {} emails in thread {}",
                    add.stream()
                            .map(IdentifiableMailboxWithRoleAndName::getName)
                            .collect(Collectors.toList()),
                    remove.stream()
                            .map(IdentifiableMailboxWithRoleAndName::getName)
                            .collect(Collectors.toList()),
                    emails.size(),
                    threadId);
        }
        return getMua().modifyLabels(emails, add, remove);
    }
}
