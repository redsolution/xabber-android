package com.xabber.android.data.message;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;

public class NewIncomingMessageEvent {
    private final AccountJid account;
    private final UserJid user;

    public NewIncomingMessageEvent(AccountJid account, UserJid user) {
        this.account = account;
        this.user = user;
    }

    public AccountJid getAccount() {
        return account;
    }

    public UserJid getUser() {
        return user;
    }
}
