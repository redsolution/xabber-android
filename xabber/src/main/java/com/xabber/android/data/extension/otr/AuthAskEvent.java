package com.xabber.android.data.extension.otr;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;

/**
 * Created by valery.miller on 04.07.17.
 */

public class AuthAskEvent {

    private AccountJid account;
    private UserJid user;

    public AuthAskEvent(AccountJid account, UserJid user) {
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
