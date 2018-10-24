package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.support.annotation.StyleRes;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.database.messagerealm.MessageItem;

public class OutgoingMessageVH extends FileMessageVH {

    OutgoingMessageVH(View itemView, MessageClickListener messageListener,
                      MessageLongClickListener longClickListener,
                      FileListener fileListener, @StyleRes int appearance) {
        super(itemView, messageListener, longClickListener, fileListener, appearance);
    }

    public void bind(MessageItem messageItem, boolean isMUC, boolean showOriginalOTR,
                     final Context context, boolean unread, boolean checked, boolean showCheckboxes) {
        super.bind(messageItem, isMUC, showOriginalOTR, context, unread, checked, showCheckboxes);

        setStatusIcon(messageItem);

        // setup PROGRESS
        progressBar.setVisibility(messageItem.isInProgress() ? View.VISIBLE : View.GONE);

        // setup BACKGROUND COLOR
        messageBalloon.getBackground().setLevel(17);

        // subscribe for FILE UPLOAD PROGRESS
        itemView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
                subscribeForUploadProgress(context);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                unsubscribeAll();
            }
        });
    }

    private void setStatusIcon(MessageItem messageItem) {
        statusIcon.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);

        boolean isFileUploadInProgress = MessageItem.isUploadFileMessage(messageItem);

        if (isFileUploadInProgress)
            progressBar.setVisibility(View.VISIBLE);

        int messageIcon = R.drawable.ic_message_delivered_14dp;
        if (messageItem.isForwarded()) {
            messageIcon = R.drawable.ic_message_forwarded_14dp;
        } else if (messageItem.isReceivedFromMessageArchive()) {
            messageIcon = R.drawable.ic_message_synced_14dp;
        } else if (messageItem.isError()) {
            messageIcon = R.drawable.ic_message_has_error_14dp;
        } else if (!isFileUploadInProgress && !messageItem.isSent()
                && System.currentTimeMillis() - messageItem.getTimestamp() > 1000) {
            messageIcon = R.drawable.ic_message_not_sent_14dp;
        } else if (!messageItem.isDelivered()) {
            if (messageItem.isAcknowledged())
                messageIcon = R.drawable.ic_message_acknowledged_14dp;
            else statusIcon.setVisibility(View.GONE);
        }
        statusIcon.setImageResource(messageIcon);
    }
}
