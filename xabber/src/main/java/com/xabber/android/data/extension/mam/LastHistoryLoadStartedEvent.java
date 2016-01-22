package com.xabber.android.data.extension.mam;

public class LastHistoryLoadStartedEvent {
    private String account;
    private String user;

    public LastHistoryLoadStartedEvent(String account, String user) {
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
