package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.message.chat.ChatAction;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.utils.StringUtils;

import java.util.Date;

public class ActionMessageVH extends BasicMessageVH {

    private TextView messageTime;

    public ActionMessageVH(View itemView) {
        super(itemView);

        messageTime = itemView.findViewById(R.id.message_time);
    }

    public void bind(MessageRealmObject messageRealmObject, Context context, AccountJid account, boolean needDate) {
        ChatAction action = MessageRealmObject.getChatAction(messageRealmObject);
        String time = StringUtils.getTimeText(new Date(messageRealmObject.getTimestamp()));

        String name = RosterManager.getInstance().getBestContact(account, messageRealmObject.getUser()).getName();
        messageText.setText(action.getText(context, name, MessageRealmObject.getSpannable(messageRealmObject).toString()));
        messageTime.setText(time);
        this.needDate = needDate;
        date = StringUtils.getDateStringForMessage(messageRealmObject.getTimestamp());
    }
}
