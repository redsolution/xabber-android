package com.xabber.android.data.extension.mam;

public class LastHistoryLoadFinishedEvent {
    private String account;
    private String user;

    public LastHistoryLoadFinishedEvent(String account, String user) {
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
