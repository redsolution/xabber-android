package com.xabber.android.ui.adapter.chat;

import static com.xabber.android.ui.helper.AndroidUtilsKt.dipToPx;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.StyleRes;

import com.xabber.android.R;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.message.MessageStatus;
import com.xabber.android.ui.helper.MessageDeliveryStatusHelper;

public class OutgoingMessageVH extends MessageVH {

    OutgoingMessageVH(View itemView, MessageClickListener messageListener,
                      MessageLongClickListener longClickListener,
                      FileListener fileListener, @StyleRes int appearance) {
        super(itemView, messageListener, longClickListener, fileListener, appearance);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void bind(MessageRealmObject messageRealmObject, MessageVhExtraData extraData) {
        super.bind(messageRealmObject, extraData);

        final Context context = itemView.getContext();
        boolean needTail = extraData.isNeedTail();

        setStatusIcon(messageRealmObject);

        // setup PROGRESS
        if (messageRealmObject.getMessageStatus().equals(MessageStatus.UPLOADING)) {
            getProgressBar().setVisibility(View.VISIBLE);
            getMessageFileInfo().setText(R.string.message_status_uploading);
            getMessageFileInfo().setVisibility(View.VISIBLE);
            getMessageTime().setVisibility(View.GONE);
            getBottomMessageTime().setVisibility(View.GONE);
        } else {
            getProgressBar().setVisibility(View.GONE);
            getMessageFileInfo().setText("");
            getMessageFileInfo().setVisibility(View.GONE);
            getMessageTime().setVisibility(View.VISIBLE);
            getBottomMessageTime().setVisibility(View.VISIBLE);
        }

        // setup FORWARDED
        boolean haveForwarded = messageRealmObject.hasForwardedMessages();
        if (haveForwarded) {
            setupForwarded(messageRealmObject, extraData);

            LinearLayout.LayoutParams forwardedParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            forwardedParams.setMargins(
                    dipToPx(1f, context),
                    dipToPx(3f, context),
                    dipToPx(12f, context),
                    dipToPx(0f, context));

            getForwardedMessagesRV().setLayoutParams(forwardedParams);
        } else if (getForwardedMessagesRV() != null) {
            getForwardedMessagesRV().setVisibility(View.GONE);
        }

        if (messageRealmObject.hasAttachments() && messageRealmObject.hasImage()) {
            needTail = false;
        }

        // setup BACKGROUND
        Drawable shadowDrawable = context.getResources().getDrawable(
                haveForwarded ? (needTail ? R.drawable.fwd_out_shadow : R.drawable.fwd_shadow)
                        : (needTail ? R.drawable.msg_out_shadow : R.drawable.msg_shadow)
        );

        shadowDrawable.setColorFilter(
                context.getResources().getColor(R.color.black),
                PorterDuff.Mode.MULTIPLY
        );

        getMessageBalloon().setBackground(
                context.getResources().getDrawable(
                        haveForwarded ? (needTail ? R.drawable.fwd_out : R.drawable.fwd)
                                : (needTail ? R.drawable.msg_out : R.drawable.msg))
        );
        getMessageShadow().setBackground(shadowDrawable);

        // setup BALLOON margins
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        layoutParams.setMargins(
                dipToPx(0f, context),
                dipToPx(haveForwarded ? 0f : 3f, context),
                dipToPx(needTail ? 0f : 11f, context),
                dipToPx(3f, context)
        );
        getMessageShadow().setLayoutParams(layoutParams);

        // setup MESSAGE padding
        getMessageBalloon().setPadding(
                dipToPx(12f, context),
                dipToPx(8f, context),
                //Utils.dipToPx(needTail ? 20f : 12f, context),
                dipToPx(needTail ? 14.5f : 6.5f, context),
                dipToPx(8f, context));

        float border = 3.5f;

        if(messageRealmObject.hasAttachments()) {
            if(messageRealmObject.hasImage()) {
                getMessageBalloon().setPadding(
                        dipToPx(border, context),
                        dipToPx(border-0.05f, context),
                        dipToPx(border, context),
                        dipToPx(border, context)
                );
                if (messageRealmObject.isAttachmentImageOnly()) {
                    getMessageTime().setTextColor(context.getResources().getColor(R.color.white));
                    getBottomMessageTime().setTextColor(context.getResources().getColor(R.color.white));
                } else {
                    getMessageInfo().setPadding(
                            0, 0, dipToPx(border+1.5f, context), dipToPx(4.7f, context)
                    );
                }
            }
        }

        // setup BACKGROUND COLOR
        setUpMessageBalloonBackground(
                getMessageBalloon(),
                extraData.getColors().getOutgoingRegularBalloonColors()
        );

        // subscribe for FILE UPLOAD PROGRESS
        itemView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) { subscribeForUploadProgress(); }
            @Override
            public void onViewDetachedFromWindow(View v) { unsubscribeAll(); }
        });

        if (getMessageText().getText().toString().trim().isEmpty()) {
            getMessageText().setVisibility(View.GONE);
        }
    }

    private void setStatusIcon(MessageRealmObject messageRealmObject) {
        getStatusIcon().setVisibility(View.VISIBLE);
        getBottomStatusIcon().setVisibility(View.VISIBLE);
        getProgressBar().setVisibility(View.GONE);

        if (messageRealmObject.getMessageStatus() == MessageStatus.UPLOADING) {
            getMessageText().setText("");
            getStatusIcon().setVisibility(View.GONE);
            getBottomStatusIcon().setVisibility(View.GONE);
        } else {
            MessageDeliveryStatusHelper.INSTANCE.setupStatusImageView(
                    messageRealmObject, getStatusIcon()
            );
            MessageDeliveryStatusHelper.INSTANCE.setupStatusImageView(
                    messageRealmObject, getBottomStatusIcon()
            );
        }
    }

}
