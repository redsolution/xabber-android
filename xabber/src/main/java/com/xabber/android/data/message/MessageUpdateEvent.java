package com.xabber.android.data.message;

import android.support.annotation.Nullable;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;

public class MessageUpdateEvent {

    @Nullable
    private AccountJid account;
    @Nullable
    private UserJid user;
    @Nullable
    private String uniqueId;

    public MessageUpdateEvent() {
    }

    public MessageUpdateEvent(@Nullable AccountJid account) {
        this.account = account;
    }

    public MessageUpdateEvent(@Nullable AccountJid account, @Nullable UserJid user) {
        this.account = account;
        this.user = user;
    }

    public MessageUpdateEvent(@Nullable AccountJid account, @Nullable UserJid user, @Nullable String uniqueId) {
        this.account = account;
        this.user = user;
        this.uniqueId = uniqueId;
    }

    @Nullable
    public AccountJid getAccount() {
        return account;
    }

    @Nullable
    public UserJid getUser() {
        return user;
    }

    @Nullable
    public String getUniqueId() {
        return uniqueId;
    }
}
