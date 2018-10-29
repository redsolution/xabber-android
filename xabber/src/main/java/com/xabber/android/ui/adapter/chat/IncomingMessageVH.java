package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.support.annotation.StyleRes;
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

    IncomingMessageVH(View itemView, MessageClickListener messageListener,
                      MessageLongClickListener longClickListener,
                      FileListener fileListener, @StyleRes int appearance) {
        super(itemView, messageListener, longClickListener, fileListener, appearance);
        avatar = itemView.findViewById(R.id.avatar);
        avatarBackground = itemView.findViewById(R.id.avatarBackground);
    }

    public void bind(MessageItem messageItem, boolean isMUC, boolean showOriginalOTR,
                     Context context, String userName, boolean unread, boolean checked, int accountColorLevel) {
        super.bind(messageItem, isMUC, showOriginalOTR, context, unread, checked);

        // setup ARCHIVED icon
        statusIcon.setVisibility(messageItem.isReceivedFromMessageArchive() ? View.VISIBLE : View.GONE);

        // setup BACKGROUND COLOR
        messageBalloon.getBackground().setLevel(accountColorLevel);

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
