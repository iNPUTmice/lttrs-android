package rs.ltt.android.ui.adapter;


import rs.ltt.jmap.common.entity.Attachment;

public interface OnAttachmentActionTriggered {
    void onOpenTriggered(String emailId, Attachment attachment);

    void onActionTriggered(String emailId, Attachment attachment);
}
