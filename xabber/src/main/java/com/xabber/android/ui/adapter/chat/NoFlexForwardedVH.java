package com.xabber.android.ui.adapter.chat;

import android.view.View;

import com.xabber.android.data.database.messagerealm.MessageItem;

public class NoFlexForwardedVH extends ForwardedVH {

    public NoFlexForwardedVH(View itemView, MessageClickListener messageListener,
                             MessageLongClickListener longClickListener, FileListener listener,
                             int appearance) {
        super(itemView, messageListener, longClickListener, listener, appearance);
    }

    @Override
    public void bind(MessageItem messageItem, MessagesAdapter.MessageExtraData extraData, String accountJid) {
        super.bind(messageItem, extraData, accountJid);
        if (messageText.getText().toString().trim().isEmpty()) messageText.setVisibility(View.GONE);
    }
}
