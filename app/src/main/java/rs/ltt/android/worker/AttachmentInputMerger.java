package rs.ltt.android.worker;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.InputMerger;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.util.AttachmentSerializer;
import rs.ltt.jmap.common.entity.Attachment;

public class AttachmentInputMerger extends InputMerger {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachmentInputMerger.class);

    @NonNull
    @Override
    public Data merge(@NonNull List<Data> inputs) {
        final Data.Builder dataBuilder = new Data.Builder();
        LOGGER.info("Merging {} inputs", inputs.size());
        final ImmutableList.Builder<Attachment> attachmentBuilder = new ImmutableList.Builder<>();
        for (final Data data : inputs) {
            if (data.hasKeyWithValueOfType(BlobUploadWorker.BLOB_ID_KEY, String.class)) {
                attachmentBuilder.add(BlobUploadWorker.getAttachment(data));
            } else {
                final byte[] bytes = data.getByteArray(AbstractCreateEmailWorker.ATTACHMENTS_KEY);
                final List<rs.ltt.jmap.common.entity.Attachment> attachments =
                        bytes == null ? null : AttachmentSerializer.of(bytes);
                if (attachments != null) {
                    attachmentBuilder.addAll(attachments);
                }
                dataBuilder.putAll(data);
            }
        }
        final ImmutableList<Attachment> attachments = attachmentBuilder.build();
        LOGGER.info("Found {} attachments", attachments.size());
        dataBuilder.putByteArray(
                AbstractCreateEmailWorker.ATTACHMENTS_KEY,
                AttachmentSerializer.toByteArray(attachments));
        return dataBuilder.build();
    }
}
