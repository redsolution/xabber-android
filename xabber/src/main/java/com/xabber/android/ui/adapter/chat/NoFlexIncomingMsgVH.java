package com.xabber.android.ui.adapter.chat;

import android.view.View;

import com.xabber.android.data.database.realmobjects.MessageRealmObject;

public class NoFlexIncomingMsgVH extends IncomingMessageVH {

    public NoFlexIncomingMsgVH(View itemView, MessageClickListener messageListener,
                               MessageLongClickListener longClickListener,
                               FileListener fileListener, BindListener listener, int appearance) {
        super(itemView, messageListener, longClickListener, fileListener, listener, appearance);
    }

    @Override
    public void bind(MessageRealmObject messageRealmObject, MessagesAdapter.MessageExtraData extraData) {
        super.bind(messageRealmObject, extraData);
        if (messageText.getText().toString().trim().isEmpty()) messageText.setVisibility(View.GONE);
    }
}
