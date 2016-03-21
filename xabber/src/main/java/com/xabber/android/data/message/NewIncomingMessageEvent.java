package com.xabber.android.data.message;

public class NewIncomingMessageEvent {
    private final String account;
    private final String user;

    public NewIncomingMessageEvent(String account, String user) {
        this.account = account;
        this.user = user;
    }

    public String getAccount() {
        return account;
    }

    public String getUser() {
        return user;
    }
}
