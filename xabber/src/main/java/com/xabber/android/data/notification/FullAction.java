package com.xabber.android.data.notification;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;

public class FullAction extends Action {

    private AccountJid accountJid;
    private UserJid userJid;

    public FullAction(Action action, AccountJid accountJid, UserJid userJid) {
        super(action.getNotificationID(), action.getReplyText(), action.getActionType());
        this.accountJid = accountJid;
        this.userJid = userJid;
    }

    public AccountJid getAccountJid() {
        return accountJid;
    }

    public UserJid getUserJid() {
        return userJid;
    }
}
