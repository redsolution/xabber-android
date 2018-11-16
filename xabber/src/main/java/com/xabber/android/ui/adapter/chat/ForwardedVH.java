package com.xabber.android.ui.adapter.chat;

import android.view.View;

import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.ColorManager;

public class ForwardedVH extends FileMessageVH {

    public ForwardedVH(View itemView, MessageClickListener messageListener,
                       MessageLongClickListener longClickListener, FileListener listener,
                       int appearance) {
        super(itemView, messageListener, longClickListener, listener, appearance);
    }

    public void bind(MessageItem messageItem, MessagesAdapter.MessageExtraData extraData) {
        super.bind(messageItem, extraData);

        // hide some elements
        statusIcon.setVisibility(View.GONE);

        // setup message author
        UserJid jid = null;
        try {
            jid = UserJid.from(messageItem.getOriginalFrom());
        } catch (UserJid.UserJidCreateException e) {
            e.printStackTrace();
        }

        String author = null;
        if (jid != null) {
            if (messageItem.isFromMUC()) author = jid.getJid().getResourceOrEmpty().toString();
            else author = RosterManager.getInstance().getNameOrBareJid(messageItem.getAccount(), jid);
        }

        if (author != null && !author.isEmpty()) {
            messageHeader.setText(author);
            messageHeader.setTextColor(ColorManager.changeColor(
                    ColorGenerator.MATERIAL.getColor(author), 0.8f));
            messageHeader.setVisibility(View.VISIBLE);
        } else messageHeader.setVisibility(View.GONE);
    }
}
