package com.xabber.android.ui.adapter.chat;

import static com.xabber.android.ui.helper.AndroidUtilsKt.dipToPx;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject;
import com.xabber.android.data.database.realmobjects.GroupMemberRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.log.LogManager;

import org.jxmpp.jid.parts.Resourcepart;

public class IncomingMessageVH  extends MessageVH {

    public ImageView avatar;
    private final BindListener listener;

    public interface BindListener { void onBind(MessageRealmObject message); }

    public interface OnMessageAvatarClickListener{ void onMessageAvatarClick(int position);}

    IncomingMessageVH(View itemView, MessageClickListener messageListener,
                      MessageLongClickListener longClickListener, FileListener fileListener,
                      BindListener listener, OnMessageAvatarClickListener avatarClickListener,
                      @StyleRes int appearance) {
        super(itemView, messageListener, longClickListener, fileListener, appearance);

        avatar = itemView.findViewById(R.id.avatar);

        avatar.setOnClickListener(v -> {
            int adapterPosition = getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                LogManager.w(this.getClass().getSimpleName(), "onClick: no position");
            } else {
                avatarClickListener.onMessageAvatarClick(adapterPosition);
            }
        });

        this.listener = listener;
    }

    @Override
    @SuppressLint("UseCompatLoadingForDrawables")
    public void bind(final MessageRealmObject messageRealmObject, MessageExtraData extraData) {

        super.bind(messageRealmObject, extraData);

        Context context = extraData.getContext();
        boolean needTail = extraData.isNeedTail();

        // setup ARCHIVED icon
        //statusIcon.setVisibility(messageItem.isReceivedFromMessageArchive() ? View.VISIBLE : View.GONE);
        statusIcon.setVisibility(View.GONE);

        // setup FORWARDED
        boolean haveForwarded = messageRealmObject.hasForwardedMessages();
        if (haveForwarded) {
            setupForwarded(messageRealmObject, extraData);

            LinearLayout.LayoutParams forwardedParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            forwardedParams.setMargins(
                    dipToPx(12f, context),
                    dipToPx(3f, context),
                    dipToPx(1f, context),
                    dipToPx(0f, context));

            forwardedMessagesRV.setLayoutParams(forwardedParams);
        } else {
            forwardedMessagesRV.setVisibility(View.GONE);
        }

        boolean imageAttached = false;
        boolean imageOnly = true;

        if(messageRealmObject.haveAttachments()) {
            for (AttachmentRealmObject a : messageRealmObject.getAttachmentRealmObjects()){
                if (a.isImage()) {
                    imageAttached = true;
                } else imageOnly = false;

                if (imageOnly) {
                    needTail = false;
                }
            }
        } else if (messageRealmObject.hasImage() && messageRealmObject.getAttachmentRealmObjects().get(0).isImage()) {
            if (getMessageText().getText().toString().trim().isEmpty()) {
                imageAttached = true;
                needTail = false; //not using the tail for messages with *only* images
            } else {
                imageAttached = true;
            }
        }

            // setup BACKGROUND
        Drawable balloonDrawable = context.getResources().getDrawable(
                haveForwarded ? (needTail ? R.drawable.fwd_in : R.drawable.fwd)
                            : (needTail ? R.drawable.msg_in : R.drawable.msg));
        Drawable shadowDrawable = context.getResources().getDrawable(
                haveForwarded ? (needTail ? R.drawable.fwd_in_shadow : R.drawable.fwd_shadow)
                            : (needTail ? R.drawable.msg_in_shadow : R.drawable.msg_shadow));
        shadowDrawable.setColorFilter(context.getResources().getColor(R.color.black),
                PorterDuff.Mode.MULTIPLY);
        messageBalloon.setBackground(balloonDrawable);
        messageShadow.setBackground(shadowDrawable);

        // setup BALLOON margins
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        layoutParams.setMargins(
                dipToPx(needTail ? 3f : 11f, context),
                dipToPx(haveForwarded ? 0f : 3f, context),
                dipToPx(0f, context),
                dipToPx(3f, context));
        messageShadow.setLayoutParams(layoutParams);

        // setup MESSAGE padding
        messageBalloon.setPadding(
                dipToPx(needTail ? 20f : 12f, context),
                dipToPx(8f, context),
                dipToPx(12f, context),
                dipToPx(8f, context));

        if (imageAttached) {
            float border = 3.5f;
            messageBalloon.setPadding(
                    dipToPx(needTail ? border + 8f : border, context),
                    dipToPx(border, context),
                    dipToPx(border, context),
                    dipToPx(border, context)
            );
            if (getMessageText().getText().toString().trim().isEmpty()
                    && messageRealmObject.isAttachmentImageOnly()
            ) {
                messageTime.setTextColor(context.getResources().getColor(R.color.white));
            }
        }

        needTail = extraData.isNeedTail(); //restoring the original tail value for the interaction with avatars

        // setup BACKGROUND COLOR
        setUpMessageBalloonBackground(messageBalloon, extraData.getColorStateList());

        setUpAvatar(context, extraData.getGroupMember(), messageRealmObject, needTail);

        // hide empty message
        if (messageRealmObject.getText().trim().isEmpty()
                && !messageRealmObject.hasForwardedMessages()
                && !messageRealmObject.haveAttachments()) {
            messageBalloon.setVisibility(View.GONE);
            messageShadow.setVisibility(View.GONE);
            messageTime.setVisibility(View.GONE);
            avatar.setVisibility(View.GONE);
            LogManager.w(this, "Empty message! Hidden, but need to correct");
        } else {
            messageBalloon.setVisibility(View.VISIBLE);
            messageTime.setVisibility(View.VISIBLE);
        }

        itemView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
                if (listener != null) listener.onBind(messageRealmObject);
            }
            @Override
            public void onViewDetachedFromWindow(View v) {
                unsubscribeAll();
            }
        });

        if (getMessageText().getText().toString().trim().isEmpty()) {
            getMessageText().setVisibility(View.GONE);
        }
    }

    private void setUpAvatar(Context context, GroupMemberRealmObject groupMember,
                             MessageRealmObject messageRealmObject, boolean needTail) {

        boolean needAvatar = SettingsManager.chatsShowAvatars();
        // for new groupchats (0GGG)
        if (groupMember != null) {
            needAvatar = true;
        }

        if (!needAvatar) {
            avatar.setVisibility(View.GONE);
            return;
        }

        if (!needTail) {
            avatar.setVisibility(View.INVISIBLE);
            return;
        }

        avatar.setVisibility(View.VISIBLE);

        //groupchat avatar
        if (groupMember != null) {
            Drawable placeholder;
            try {
                ContactJid contactJid = ContactJid.from(messageRealmObject.getUser().getJid().toString()
                        + "/"
                        + groupMember.getNickname());
                placeholder = AvatarManager.getInstance().getOccupantAvatar(
                        contactJid, groupMember.getNickname()
                );

            } catch (ContactJid.ContactJidCreateException e) {
               placeholder = AvatarManager.getInstance().generateDefaultAvatar(
                       groupMember.getNickname(), groupMember.getNickname()
               );
            }
            Glide.with(context)
                    .load(
                            AvatarManager.getInstance().getGroupMemberAvatar(
                                    groupMember, messageRealmObject.getAccount()
                            )
                    )
                    .centerCrop()
                    .placeholder(placeholder)
                    .error(placeholder)
                    .into(avatar);
            return;
        }

        final ContactJid user = messageRealmObject.getUser();
        final Resourcepart resource = messageRealmObject.getResource();

        if (resource.equals(Resourcepart.EMPTY)) {
            avatar.setImageDrawable(AvatarManager.getInstance().getRoomAvatarForContactList(user));
        } else {

            String nick = resource.toString();
            ContactJid contactJid;

            try {
                contactJid = ContactJid.from(user.getJid().toString() + "/" + resource.toString());
                avatar.setImageDrawable(
                        AvatarManager.getInstance().getOccupantAvatar(contactJid, nick)
                );
            } catch (ContactJid.ContactJidCreateException e) {
                LogManager.exception(this, e);
                avatar.setImageDrawable(
                        AvatarManager.getInstance().generateDefaultAvatar(nick, nick)
                );
            }
        }
    }

}
