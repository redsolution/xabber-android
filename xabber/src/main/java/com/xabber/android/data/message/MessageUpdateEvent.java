package com.xabber.android.data.message;

public class MessageUpdateEvent {


    private String account;
    private String user;
    private String uniqueId;

    public MessageUpdateEvent(String account, String user, String uniqueId) {
        this.account = account;
        this.user = user;
        this.uniqueId = uniqueId;
    }

    public String getAccount() {
        return account;
    }

    public String getUser() {
        return user;
    }

    public String getUniqueId() {
        return uniqueId;
    }
}
