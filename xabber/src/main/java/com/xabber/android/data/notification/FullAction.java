package com.xabber.android.data.notification;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;

public class FullAction extends Action {

    private final AccountJid accountJid;
    private final ContactJid contactJid;

    public FullAction(Action action, AccountJid accountJid, ContactJid contactJid) {
        super(action.getNotificationID(), action.getReplyText(), action.getActionType());
        this.accountJid = accountJid;
        this.contactJid = contactJid;
    }

    public AccountJid getAccountJid() {
        return accountJid;
    }

    public ContactJid getContactJid() {
        return contactJid;
    }
}
