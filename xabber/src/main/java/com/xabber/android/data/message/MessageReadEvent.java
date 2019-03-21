package com.xabber.android.data.message;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;

import java.util.List;

public class MessageReadEvent  {

    private AccountJid account;
    private UserJid user;
    private List<String> ids;

    public MessageReadEvent(AccountJid account, UserJid user, List<String> ids) {
        this.account = account;
        this.user = user;
        this.ids = ids;
    }

    public AccountJid getAccount() {
        return account;
    }

    public UserJid getUser() {
        return user;
    }

    public List<String> getIds() {
        return ids;
    }
}
