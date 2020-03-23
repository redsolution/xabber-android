package com.xabber.android.data.extension.otr;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;

/**
 * Created by valery.miller on 04.07.17.
 */

public class AuthAskEvent {

    private AccountJid account;
    private ContactJid user;

    public AuthAskEvent(AccountJid account, ContactJid user) {
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
