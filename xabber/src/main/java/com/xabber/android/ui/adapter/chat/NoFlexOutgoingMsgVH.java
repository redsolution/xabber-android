package com.xabber.android.ui.adapter.chat;

import android.view.View;

import com.xabber.android.data.database.messagerealm.MessageItem;

public class NoFlexOutgoingMsgVH extends OutgoingMessageVH {

    public NoFlexOutgoingMsgVH(View itemView, MessageClickListener messageListener,
                               MessageLongClickListener longClickListener,
                               FileListener fileListener, int appearance) {
        super(itemView, messageListener, longClickListener, fileListener, appearance);
    }

    @Override
    public void bind(MessageItem messageItem, MessagesAdapter.MessageExtraData extraData) {
        super.bind(messageItem, extraData);
        if (messageText.getText().toString().trim().isEmpty()) messageText.setVisibility(View.GONE);
    }
}
