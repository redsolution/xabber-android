package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.StyleRes;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.xabber.android.R;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.utils.Utils;

public class OutgoingMessageVH extends FileMessageVH {

    OutgoingMessageVH(View itemView, MessageClickListener messageListener,
                      MessageLongClickListener longClickListener,
                      FileListener fileListener, @StyleRes int appearance) {
        super(itemView, messageListener, longClickListener, fileListener, appearance);
    }

    public void bind(MessageItem messageItem, MessagesAdapter.MessageExtraData extraData) {
        super.bind(messageItem, extraData);

        final Context context = extraData.getContext();
        boolean needTail = extraData.isNeedTail();

        setStatusIcon(messageItem);

        // setup PROGRESS
        progressBar.setVisibility(messageItem.isInProgress() ? View.VISIBLE : View.GONE);

        // setup FORWARDED
        boolean haveForwarded = messageItem.haveForwardedMessages();
        if (haveForwarded) {
            setupForwarded(messageItem, extraData);

            LinearLayout.LayoutParams forwardedParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            forwardedParams.setMargins(
                    Utils.dipToPx(1f, context),
                    Utils.dipToPx(2f, context),
                    Utils.dipToPx(needTail ? 11f : 12f, context),
                    Utils.dipToPx(0f, context));

            forwardLayout.setLayoutParams(forwardedParams);
        } else forwardLayout.setVisibility(View.GONE);

        // setup BACKGROUND
        Drawable shadowDrawable = context.getResources().getDrawable(
                haveForwarded ? (needTail ? R.drawable.fwd_out_shadow : R.drawable.fwd_shadow)
                        : (needTail ? R.drawable.msg_out_shadow : R.drawable.msg_shadow));
        shadowDrawable.setColorFilter(context.getResources().getColor(R.color.black), PorterDuff.Mode.MULTIPLY);
        messageBalloon.setBackgroundDrawable(context.getResources().getDrawable(
                haveForwarded ? (needTail ? R.drawable.fwd_out : R.drawable.fwd)
                            : (needTail ? R.drawable.msg_out : R.drawable.msg)));
        messageShadow.setBackgroundDrawable(shadowDrawable);

        // setup BALLOON margins
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        layoutParams.setMargins(
                Utils.dipToPx(0f, context),
                Utils.dipToPx(haveForwarded ? 0f : 2f, context),
                Utils.dipToPx(needTail ? 2f : 11f, context),
                Utils.dipToPx(2f, context));
        messageShadow.setLayoutParams(layoutParams);

        // setup MESSAGE padding
        messageBalloon.setPadding(
                Utils.dipToPx(12f, context),
                Utils.dipToPx(8f, context),
                Utils.dipToPx(needTail ? 20f : 12f, context),
                Utils.dipToPx(8f, context));

        // setup BACKGROUND COLOR
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.message_background, typedValue, true);
        setUpMessageBalloonBackground(messageBalloon, context.getResources().getColorStateList(typedValue.resourceId));

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
