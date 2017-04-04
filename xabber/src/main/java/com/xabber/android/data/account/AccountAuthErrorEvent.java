package com.xabber.android.data.account;

import com.xabber.android.data.entity.AccountJid;

import org.jivesoftware.smack.sasl.SASLErrorException;



public class AccountAuthErrorEvent {
    private final AccountJid account;
    private final SASLErrorException saslErrorException;

    public AccountAuthErrorEvent(AccountJid account, SASLErrorException saslErrorException) {

        this.account = account;
        this.saslErrorException = saslErrorException;
    }

    public AccountJid getAccount() {
        return account;
    }

    public SASLErrorException getSaslErrorException() {
        return saslErrorException;
    }
}
