package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.StyleRes;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
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
                     final Context context, boolean unread, AccountJid account) {
        super.bind(messageItem, isMUC, showOriginalOTR, context, unread);

        setStatusIcon(messageItem);

        // setup PROGRESS
        progressBar.setVisibility(messageItem.isInProgress() ? View.VISIBLE : View.GONE);

        // setup BACKGROUND COLOR
        setUpMessageBalloonBackground(messageBalloon,
                context.getResources().getColorStateList(R.color.outgoing_message_color_state_dark),
                R.drawable.message_outgoing_states, account);

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

    private void setUpMessageBalloonBackground(
            View messageBalloon, ColorStateList darkColorStateList,
            int lightBackgroundId, AccountJid account) {

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
            final Drawable originalBackgroundDrawable = messageBalloon.getBackground();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                originalBackgroundDrawable.setTintList(darkColorStateList);
            } else {
                Drawable wrapDrawable = DrawableCompat.wrap(originalBackgroundDrawable);
                DrawableCompat.setTintList(wrapDrawable, darkColorStateList);

                int pL = messageBalloon.getPaddingLeft();
                int pT = messageBalloon.getPaddingTop();
                int pR = messageBalloon.getPaddingRight();
                int pB = messageBalloon.getPaddingBottom();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    messageBalloon.setBackground(wrapDrawable);
                } else {
                    messageBalloon.setBackgroundDrawable(wrapDrawable);
                }

                messageBalloon.setPadding(pL, pT, pR, pB);
            }
        } else {
            int pL = messageBalloon.getPaddingLeft();
            int pT = messageBalloon.getPaddingTop();
            int pR = messageBalloon.getPaddingRight();
            int pB = messageBalloon.getPaddingBottom();

            messageBalloon.setBackgroundResource(lightBackgroundId);
            messageBalloon.getBackground().setLevel(AccountManager.getInstance().getColorLevel(account));
            messageBalloon.setPadding(pL, pT, pR, pB);
        }
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
