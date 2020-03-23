package com.xabber.android.data.message;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;

public class NewIncomingMessageEvent {
    private final AccountJid account;
    private final ContactJid user;

    public NewIncomingMessageEvent(AccountJid account, ContactJid user) {
        this.account = account;
        this.user = user;
    }

    public AccountJid getAccount() {
        return account;
    }

    public ContactJid getUser() {
        return user;
    }
}
