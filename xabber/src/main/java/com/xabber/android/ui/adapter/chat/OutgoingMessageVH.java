package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.support.annotation.StyleRes;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;

import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

public class OutgoingMessageVH extends FileMessageVH {

    private CompositeSubscription subscriptions = new CompositeSubscription();

    TextView messageFileInfo;
    ProgressBar progressBar;

    OutgoingMessageVH(View itemView, MessageClickListener listener, @StyleRes int appearance) {
        super(itemView, listener, appearance);
        progressBar = (ProgressBar) itemView.findViewById(R.id.message_progress_bar);
        messageFileInfo = (TextView) itemView.findViewById(R.id.message_file_info);
    }

    public void bind(MessageItem messageItem, boolean isMUC, boolean showOriginalOTR,
                     final Context context, boolean unread) {
        super.bind(messageItem, isMUC, showOriginalOTR, context, unread);

        setStatusIcon(messageItem);

        // setup PROGRESS
        progressBar.setVisibility(messageItem.isInProgress() ? View.VISIBLE : View.GONE);

        // setup BACKGROUND COLOR
        messageBalloon.getBackground().setLevel(17);

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

    private void subscribeForUploadProgress(final Context context) {
        subscriptions.add(HttpFileUploadManager.getInstance().subscribeForProgress()
            .doOnNext(new Action1<HttpFileUploadManager.ProgressData>() {
                @Override
                public void call(HttpFileUploadManager.ProgressData progressData) {
                    setUpProgress(context, progressData);
                }
            }).subscribe());
    }

    private void unsubscribeAll() {
        subscriptions.clear();
    }

    private void setUpProgress(Context context, HttpFileUploadManager.ProgressData progressData) {
        if (progressData != null && messageId.equals(progressData.getMessageId())) {
            if (progressData.isCompleted()) {
                showProgress(false);
            } else if (progressData.getError() != null) {
                showProgress(false);
                onClickListener.onDownloadError(progressData.getError());
            } else {
                if (uploadProgressBar != null) uploadProgressBar.setProgress(progressData.getProgress());
                if (messageFileInfo != null)
                    messageFileInfo.setText(context.getString(R.string.uploaded_files_count,
                            progressData.getProgress() + "/" + progressData.getFileCount()));
                showProgress(true);
            }
        } else showProgress(false);
    }

    private void showProgress(boolean show) {
        if (uploadProgressBar != null) uploadProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (ivCancelUpload != null) ivCancelUpload.setVisibility(show ? View.VISIBLE : View.GONE);
        if (messageFileInfo != null) messageFileInfo.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
