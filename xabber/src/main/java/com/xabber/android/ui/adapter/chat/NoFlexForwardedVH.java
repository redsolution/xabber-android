package com.xabber.android.ui.adapter.chat;

import android.view.View;

import com.xabber.android.data.database.realmobjects.MessageRealmObject;

public class NoFlexForwardedVH extends ForwardedVH {

    public NoFlexForwardedVH(View itemView, MessageClickListener messageListener,
                             MessageLongClickListener longClickListener, FileListener listener, int appearance) {
        super(itemView, messageListener, longClickListener, listener, appearance);
    }

    @Override
    public void bind(MessageRealmObject messageRealmObject, MessageExtraData extraData,
                     String accountJid) {

        super.bind(messageRealmObject, extraData, accountJid);
        if (messageText.getText().toString().trim().isEmpty()) messageText.setVisibility(View.GONE);
    }

}
