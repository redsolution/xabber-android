package com.xabber.android.data.message;

import androidx.annotation.Nullable;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;

public class MessageUpdateEvent {

    @Nullable
    private AccountJid account;
    @Nullable
    private ContactJid user;
    @Nullable
    private String uniqueId;

    public MessageUpdateEvent() {
    }

    public MessageUpdateEvent(@Nullable AccountJid account) {
        this.account = account;
    }

    public MessageUpdateEvent(@Nullable AccountJid account, @Nullable ContactJid user) {
        this.account = account;
        this.user = user;
    }

    public MessageUpdateEvent(@Nullable AccountJid account, @Nullable ContactJid user, @Nullable String uniqueId) {
        this.account = account;
        this.user = user;
        this.uniqueId = uniqueId;
    }

    @Nullable
    public AccountJid getAccount() {
        return account;
    }

    @Nullable
    public ContactJid getUser() {
        return user;
    }

    @Nullable
    public String getUniqueId() {
        return uniqueId;
    }
}
