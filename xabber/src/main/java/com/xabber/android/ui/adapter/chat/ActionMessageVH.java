package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
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

    public void bind(MessageItem messageItem, Context context, AccountJid account, boolean isMUC) {
        ChatAction action = MessageItem.getChatAction(messageItem);
        String time = StringUtils.getSmartTimeText(context, new Date(messageItem.getTimestamp()));

        String name;
        if (isMUC) {
            name = messageItem.getResource().toString();
        } else {
            name = RosterManager.getInstance().getBestContact(account, messageItem.getUser()).getName();
        }
        int color;
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
            color = messageText.getContext().getResources().getColor(R.color.grey_700);
        } else {
            color = messageText.getContext().getResources().getColor(R.color.grey_500);
        }
        messageText.setText(action.getText(context, name, MessageItem.getSpannable(messageItem).toString()));
        messageTime.setText(time);
    }
}
