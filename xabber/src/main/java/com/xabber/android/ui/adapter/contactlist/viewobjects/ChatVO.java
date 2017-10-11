package com.xabber.android.ui.adapter.contactlist.viewobjects;

import android.graphics.drawable.Drawable;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;

/**
 * Created by valery.miller on 11.10.17.
 */

public class ChatVO extends ContactVO {

    private String messageText;
    private boolean isOutgoing;
    private String time;

    public ChatVO(int accountColorIndicator, boolean showOfflineShadow, String name, String status,
                  int statusId, int statusLevel, Drawable avatar, int mucIndicatorLevel,
                  UserJid userJid, AccountJid accountJid, String messageText, boolean isOutgoing,
                  String time) {

        super(accountColorIndicator, showOfflineShadow, name, status, statusId, statusLevel,
                avatar, mucIndicatorLevel, userJid, accountJid);

        this.messageText = messageText;
        this.isOutgoing = isOutgoing;
        this.time = time;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public boolean isOutgoing() {
        return isOutgoing;
    }

    public void setOutgoing(boolean outgoing) {
        isOutgoing = outgoing;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
