package com.xabber.android.ui.adapter.chat;

import android.view.View;

import com.xabber.android.data.database.messagerealm.MessageItem;

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
    }
}
