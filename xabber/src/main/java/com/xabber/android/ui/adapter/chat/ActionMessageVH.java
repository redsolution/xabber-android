package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.view.View;

import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.message.ChatAction;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.utils.StringUtils;

import java.util.Date;

public class ActionMessageVH extends BasicMessageVH {

    public ActionMessageVH(View itemView) {
        super(itemView);
    }

    public void bind(MessageItem messageItem, Context context, AccountJid account, boolean isMUC) {
        ChatAction action = MessageItem.getChatAction(messageItem);
        String time = StringUtils.getSmartTimeText(context, new Date(messageItem.getTimestamp()));

        String name;
        if (isMUC) {
            name = messageItem.getResource().toString();
        } else {
            name = RosterManager.getInstance().getBestContact(account, messageItem.getUser()).getName();
        }
        messageText.setText(time + ": "
                + action.getText(context, name, MessageItem.getSpannable(messageItem).toString()));
    }
}
