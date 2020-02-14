package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.database.realmobjects.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.message.ChatAction;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.utils.StringUtils;

import java.util.Date;

public class ActionMessageVH extends BasicMessageVH {

    private TextView messageTime;

    public ActionMessageVH(View itemView) {
        super(itemView);

        messageTime = itemView.findViewById(R.id.message_time);
    }

    public void bind(MessageItem messageItem, Context context, AccountJid account, boolean isMUC, boolean needDate) {
        ChatAction action = MessageItem.getChatAction(messageItem);
        String time = StringUtils.getTimeText(new Date(messageItem.getTimestamp()));

        String name;
        if (isMUC) {
            name = messageItem.getResource().toString();
        } else {
            name = RosterManager.getInstance().getBestContact(account, messageItem.getUser()).getName();
        }
        messageText.setText(action.getText(context, name, MessageItem.getSpannable(messageItem).toString()));
        messageTime.setText(time);
        this.needDate = needDate;
        date = StringUtils.getDateStringForMessage(messageItem.getTimestamp());
    }
}
