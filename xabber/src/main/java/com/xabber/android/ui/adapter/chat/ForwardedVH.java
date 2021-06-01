package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.utils.Utils;

public class ForwardedVH extends FileMessageVH {

    private TextView tvForwardedCount;

    public ForwardedVH(View itemView, MessageClickListener messageListener,
                       MessageLongClickListener longClickListener, FileListener listener,
                       int appearance) {
        super(itemView, messageListener, longClickListener, listener, appearance);
        tvForwardedCount = itemView.findViewById(R.id.tvForwardedCount);
    }

    public void bind(MessageRealmObject messageRealmObject, MessagesAdapter.MessageExtraData extraData, String accountJid) {
        super.bind(messageRealmObject, extraData);

        // hide STATUS ICONS
        statusIcon.setVisibility(View.GONE);

        // setup MESSAGE AUTHOR
        ContactJid jid = null;
        try {
            jid = ContactJid.from(messageRealmObject.getOriginalFrom());
        } catch (ContactJid.UserJidCreateException e) {
            e.printStackTrace();
        }
        String author = RosterManager.getDisplayAuthorName(messageRealmObject);
        if (extraData.getGroupchatUser() != null)
            author = extraData.getGroupchatUser().getNickname();

        if (author != null && !author.isEmpty()) {
            messageHeader.setText(author);
            messageHeader.setTextColor(ColorManager.changeColor(
                    ColorGenerator.MATERIAL.getColor(author), 0.8f));
            messageHeader.setVisibility(View.VISIBLE);
        } else messageHeader.setVisibility(View.GONE);

        // setup FORWARDED
        Context context = extraData.getContext();
        boolean haveForwarded = messageRealmObject.haveForwardedMessages();
        if (haveForwarded) {
            forwardLayout.setVisibility(View.VISIBLE);
            tvForwardedCount.setText(String.format(extraData.getContext()
                    .getResources().getString(R.string.forwarded_messages_count), messageRealmObject.getForwardedIds().size()));
            tvForwardedCount.setPaintFlags(tvForwardedCount.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            forwardLayout.setBackgroundColor(ColorManager.getColorWithAlpha(R.color.forwarded_background_color, 0.2f));
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light){
                forwardLeftBorder.setBackgroundColor(extraData.getAccountMainColor());
                forwardLeftBorder.setAlpha(1);
                tvForwardedCount.setAlpha(1);
            }
            else{
                forwardLeftBorder.setBackgroundColor(ColorManager.getInstance()
                        .getAccountPainter().getAccountColorWithTint(messageRealmObject.getAccount(), 900));
                forwardLeftBorder.setAlpha(0.6f);
                tvForwardedCount.setAlpha(0.6f);
            }
        } else forwardLayout.setVisibility(View.GONE);

        // setup BACKGROUND
        Drawable balloonDrawable = context.getResources().getDrawable(
                haveForwarded ? R.drawable.fwd : R.drawable.msg);
        Drawable shadowDrawable = context.getResources().getDrawable(
                haveForwarded ? R.drawable.fwd_shadow : R.drawable.msg_shadow);
        shadowDrawable.setColorFilter(context.getResources().getColor(R.color.black), PorterDuff.Mode.MULTIPLY);
        messageBalloon.setBackgroundDrawable(balloonDrawable);
        messageShadow.setBackgroundDrawable(shadowDrawable);


        float border = 3.5f;
        if(messageRealmObject.haveAttachments()) {
            if(messageRealmObject.isAttachmentImageOnly()) {
                messageBalloon.setPadding(
                        Utils.dipToPx(border, context),
                        Utils.dipToPx(border, context),
                        Utils.dipToPx(border, context),
                        Utils.dipToPx(border, context));
                /*messageBalloon.setPadding(
                        Utils.dipToPx(3f, context),
                        Utils.dipToPx(-2f, context),
                        Utils.dipToPx(3f, context),
                        Utils.dipToPx(-15f, context));*/
                /*messageInfo.setPadding(
                        Utils.dipToPx(0f, context),
                        Utils.dipToPx(-7f, context),
                        Utils.dipToPx(0f, context),
                        Utils.dipToPx(0f, context));*/
            }
        }

        // setup BACKGROUND COLOR
        if (jid != null && !accountJid.equals(jid.getBareJid().toString()))
            setUpMessageBalloonBackground(messageBalloon, extraData.getColorStateList());
        else {
            TypedValue typedValue = new TypedValue();
            extraData.getContext().getTheme().resolveAttribute(R.attr.message_background,
                    typedValue, true);
            setUpMessageBalloonBackground(messageBalloon,
                    extraData.getContext().getResources().getColorStateList(typedValue.resourceId));
        }

    }
}
