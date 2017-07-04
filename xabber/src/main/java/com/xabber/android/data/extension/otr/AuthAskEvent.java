package com.xabber.android.data.extension.otr;

import android.content.Intent;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;

/**
 * Created by valery.miller on 04.07.17.
 */

public class AuthAskEvent {

    private AccountJid account;
    private UserJid user;
    private Intent intent;

    public AuthAskEvent(AccountJid account, UserJid user, Intent intent) {
        this.account = account;
        this.user = user;
        this.intent = intent;
    }

    public AccountJid getAccount() {
        return account;
    }

    public UserJid getUser() {
        return user;
    }

    public Intent getIntent() {
        return intent;
    }
}
