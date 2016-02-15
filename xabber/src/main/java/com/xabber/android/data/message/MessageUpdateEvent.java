package com.xabber.android.data.message;

import android.support.annotation.Nullable;

public class MessageUpdateEvent {

    @Nullable
    private String account;
    @Nullable
    private String user;
    @Nullable
    private String uniqueId;

    public MessageUpdateEvent() {
    }

    public MessageUpdateEvent(String account) {
        this.account = account;
    }

    public MessageUpdateEvent(String account, String user) {
        this.account = account;
        this.user = user;
    }

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
