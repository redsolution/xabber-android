package com.xabber.android.ui.adapter.chat;

import android.view.View;

import com.xabber.android.data.database.messagerealm.MessageItem;

public class NoFlexIncomingMsgVH extends IncomingMessageVH {

    public NoFlexIncomingMsgVH(View itemView, MessageClickListener messageListener,
                               MessageLongClickListener longClickListener,
                               FileListener fileListener, BindListener listener, int appearance) {
        super(itemView, messageListener, longClickListener, fileListener, listener, appearance);
    }

    @Override
    public void bind(MessageItem messageItem, MessagesAdapter.MessageExtraData extraData) {
        super.bind(messageItem, extraData);
        if (messageText.getText().toString().trim().isEmpty()) messageText.setVisibility(View.GONE);
    }
}
