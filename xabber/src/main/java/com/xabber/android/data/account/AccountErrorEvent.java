package com.xabber.android.data.account;

import com.xabber.android.data.entity.AccountJid;

import java.io.Serializable;


public class AccountErrorEvent implements Serializable {
    private final AccountJid account;
    private final Type type;
    private final String message;

    public enum Type implements Serializable {
        AUTHORIZATION,
        CONNECTION,
        PASS_REQUIRED
    }

    public AccountErrorEvent(AccountJid account, Type type, String message) {
        this.account = account;
        this.type = type;
        this.message = message;
    }

    public AccountJid getAccount() {
        return account;
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
