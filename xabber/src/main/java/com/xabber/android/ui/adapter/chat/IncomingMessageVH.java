package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.StyleRes;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;
import android.widget.ImageView;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.log.LogManager;

import org.jxmpp.jid.parts.Resourcepart;

public class IncomingMessageVH  extends FileMessageVH {

    public ImageView avatar;
    public ImageView avatarBackground;

    IncomingMessageVH(View itemView, MessageClickListener listener, @StyleRes int appearance) {
        super(itemView, listener, appearance);
        avatar = (ImageView) itemView.findViewById(R.id.avatar);
        avatarBackground = (ImageView) itemView.findViewById(R.id.avatarBackground);
    }

    public void bind(MessageItem messageItem, boolean isMUC, boolean showOriginalOTR,
                     Context context, String userName, boolean unread, ColorStateList backgroundColors) {
        super.bind(messageItem, isMUC, showOriginalOTR, context, unread);

        // setup ARCHIVED icon
        statusIcon.setVisibility(messageItem.isReceivedFromMessageArchive() ? View.VISIBLE : View.GONE);

        // setup BACKGROUND COLOR
        setUpMessageBalloonBackground(messageBalloon, backgroundColors);

        setUpAvatar(messageItem, isMUC, userName);

        // hide empty message
        if (messageItem.getText().trim().isEmpty()) {
            messageBalloon.setVisibility(View.GONE);
            messageTime.setVisibility(View.GONE);
            avatar.setVisibility(View.GONE);
            avatarBackground.setVisibility(View.GONE);
            LogManager.w(this, "Empty message! Hidden, but need to correct");
        } else {
            messageBalloon.setVisibility(View.VISIBLE);
            messageTime.setVisibility(View.VISIBLE);
        }
    }

    private void setUpMessageBalloonBackground(View view, ColorStateList colorList) {
        final Drawable originalBackgroundDrawable = view.getBackground();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            originalBackgroundDrawable.setTintList(colorList);

        } else {
            Drawable wrapDrawable = DrawableCompat.wrap(originalBackgroundDrawable);
            DrawableCompat.setTintList(wrapDrawable, colorList);

            int pL = view.getPaddingLeft();
            int pT = view.getPaddingTop();
            int pR = view.getPaddingRight();
            int pB = view.getPaddingBottom();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                view.setBackground(wrapDrawable);
            else view.setBackgroundDrawable(wrapDrawable);
            view.setPadding(pL, pT, pR, pB);
        }

    }

    private void setUpAvatar(MessageItem messageItem, boolean isMUC, String userName) {
        if (SettingsManager.chatsShowAvatars() && !isMUC) {
            final UserJid user = messageItem.getUser();
            avatar.setVisibility(View.VISIBLE);
            avatarBackground.setVisibility(View.VISIBLE);
            avatar.setImageDrawable(AvatarManager.getInstance().getUserAvatar(user, userName));

        } else if (SettingsManager.chatsShowAvatarsMUC() && isMUC) {
            final AccountJid account = messageItem.getAccount();
            final UserJid user = messageItem.getUser();
            final Resourcepart resource = messageItem.getResource();

            avatar.setVisibility(View.VISIBLE);
            avatarBackground.setVisibility(View.VISIBLE);
            if ((MUCManager.getInstance().getNickname(account, user.getJid().asEntityBareJidIfPossible()).equals(resource))) {
                avatar.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(account));
            } else {
                if (resource.equals(Resourcepart.EMPTY)) {
                    avatar.setImageDrawable(AvatarManager.getInstance().getRoomAvatar(user));
                } else {

                    String nick = resource.toString();
                    UserJid userJid = null;

                    try {
                        userJid = UserJid.from(user.getJid().toString() + "/" + resource.toString());
                        avatar.setImageDrawable(AvatarManager.getInstance()
                                .getOccupantAvatar(userJid, nick));

                    } catch (UserJid.UserJidCreateException e) {
                        LogManager.exception(this, e);
                        avatar.setImageDrawable(AvatarManager.getInstance()
                                .generateDefaultAvatar(nick, nick));
                    }
                }
            }
        } else {
            avatar.setVisibility(View.GONE);
            avatarBackground.setVisibility(View.GONE);
        }
    }
}
