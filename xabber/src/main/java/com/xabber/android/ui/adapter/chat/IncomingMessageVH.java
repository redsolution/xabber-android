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
import com.xabber.android.data.database.realmobjects.ReferenceRealmObject;
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
    public void bind(final MessageRealmObject messageRealmObject, MessageVhExtraData extraData) {

        super.bind(messageRealmObject, extraData);

        Context context = itemView.getContext();
        boolean needTail = extraData.isNeedTail();

        // setup ARCHIVED icon
        //statusIcon.setVisibility(messageItem.isReceivedFromMessageArchive() ? View.VISIBLE : View.GONE);
        getStatusIcon().setVisibility(View.GONE);
        getBottomStatusIcon().setVisibility(View.GONE);

        // setup FORWARDED
        boolean haveForwarded = messageRealmObject.hasForwardedMessages();
        if (haveForwarded) {
            setupForwarded(messageRealmObject, extraData);

            LinearLayout.LayoutParams forwardedParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            //todo there are problem with forward layouts
            forwardedParams.setMargins(
                    dipToPx(0f, context),
                    dipToPx(0f, context),
                    dipToPx(4f, context),
                    dipToPx(0f, context));

            getForwardedMessagesRV().setLayoutParams(forwardedParams);
        } else {
            getForwardedMessagesRV().setVisibility(View.GONE);
        }

        boolean imageAttached = false;
        boolean imageOnly = true;

        if(messageRealmObject.hasReferences()) {
            for (ReferenceRealmObject a : messageRealmObject.getReferencesRealmObjects()){
                if (a.isImage()) {
                    imageAttached = true;
                } else imageOnly = false;

                if (imageOnly) {
                    needTail = false;
                }
            }
        } else if (messageRealmObject.hasImage() && messageRealmObject.getReferencesRealmObjects().get(0).isImage()) {
            if (getMessageTextTv().getText().toString().trim().isEmpty()) {
                imageAttached = true;
                needTail = false; //not using the tail for messages with *only* images
            } else {
                imageAttached = true;
            }
        }

            // setup BACKGROUND
        Drawable balloonDrawable = context.getResources().getDrawable(
                (needTail ? R.drawable.msg_in : R.drawable.msg)
        );
        Drawable shadowDrawable = context.getResources().getDrawable(
                (needTail ? R.drawable.msg_in_shadow : R.drawable.msg_shadow)
        );
        shadowDrawable.setColorFilter(context.getResources().getColor(R.color.black),
                PorterDuff.Mode.MULTIPLY);
        getMessageBalloon().setBackground(balloonDrawable);
        getMessageShadow().setBackground(shadowDrawable);

        // setup BALLOON margins
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        layoutParams.setMargins(
                dipToPx(needTail ? 3f : 11f, context),
                dipToPx(haveForwarded ? 0f : 3f, context),
                dipToPx(0f, context),
                dipToPx(3f, context));
        getMessageShadow().setLayoutParams(layoutParams);

        // setup MESSAGE padding
        getMessageBalloon().setPadding(
                dipToPx(needTail ? 20f : 12f, context),
                dipToPx(8f, context),
                dipToPx(8f, context),
                dipToPx(8f, context));

        if (imageAttached) {
            float border = 3.5f;
            getMessageBalloon().setPadding(
                    dipToPx(needTail ? border + 8f : border, context),
                    dipToPx(border, context),
                    dipToPx(border, context),
                    dipToPx(border, context)
            );
            if (getMessageTextTv().getText().toString().trim().isEmpty()
                    && messageRealmObject.isAttachmentImageOnly()
            ) {
                getMessageTime().setTextColor(context.getResources().getColor(R.color.white));
                getBottomMessageTime().setTextColor(context.getResources().getColor(R.color.white));
            }
        }

        needTail = extraData.isNeedTail(); //restoring the original tail value for the interaction with avatars

        // setup BACKGROUND COLOR
        setUpMessageBalloonBackground(getMessageBalloon(), extraData.getColors().getIncomingRegularBalloonColors());

        setUpAvatar(context, extraData.getGroupMember(), messageRealmObject, needTail);

        // hide empty message
        if (messageRealmObject.getText().trim().isEmpty()
                && !messageRealmObject.hasForwardedMessages()
                && !messageRealmObject.hasReferences()) {
            getMessageBalloon().setVisibility(View.GONE);
            getMessageShadow().setVisibility(View.GONE);
            getMessageTime().setVisibility(View.GONE);
            getBottomMessageTime().setVisibility(View.GONE);
            avatar.setVisibility(View.GONE);
            LogManager.w(this, "Empty message! Hidden, but need to correct");
        } else {
            getMessageBalloon().setVisibility(View.VISIBLE);
            getMessageTime().setVisibility(View.VISIBLE);
            getBottomMessageTime().setVisibility(View.VISIBLE);
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

        if (getMessageTextTv().getText().toString().trim().isEmpty()) {
            getMessageTextTv().setVisibility(View.GONE);
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
