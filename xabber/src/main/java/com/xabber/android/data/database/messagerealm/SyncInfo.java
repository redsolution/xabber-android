package com.xabber.android.data.database.messagerealm;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;

import io.realm.RealmObject;
import io.realm.annotations.Index;

public class SyncInfo extends RealmObject {

    public static final String FIELD_ACCOUNT = "account";
    public static final String FIELD_USER = "user";
    public static final String FIELD_FIRST_MAM_MESSAGE_MAM_ID = "firstMamMessageMamId";
    public static final String FIELD_FIRST_MAM_MESSAGE_STANZA_ID = "firstMamMessageStanzaId";
    public static final String FIELD_LAST_MESSAGE_MAM_ID = "lastMessageMamId";
    public static final String FIELD_REMOTE_HISTORY_COMPLETELY_LOADED = "isRemoteHistoryCompletelyLoaded";

    @Index
    private String account;
    @Index
    private String user;

    private String firstMamMessageMamId;
    private String firstMamMessageStanzaId;
    private String lastMessageMamId;
    private boolean isRemoteHistoryCompletelyLoaded = false;


    public String getAccount() {
        return account;
    }

    public void setAccount(AccountJid account) {
        this.account = account.toString();
    }

    public String getUser() {
        return user;
    }

    public void setUser(UserJid user) {
        this.user = user.toString();
    }

    public String getFirstMamMessageMamId() {
        return firstMamMessageMamId;
    }

    public void setFirstMamMessageMamId(String firstMamMessageMamId) {
        this.firstMamMessageMamId = firstMamMessageMamId;
    }

    public String getLastMessageMamId() {
        return lastMessageMamId;
    }

    public void setLastMessageMamId(String lastMessageMamId) {
        this.lastMessageMamId = lastMessageMamId;
    }

    public boolean isRemoteHistoryCompletelyLoaded() {
        return isRemoteHistoryCompletelyLoaded;
    }

    public void setRemoteHistoryCompletelyLoaded(boolean remoteHistoryCompletelyLoaded) {
        isRemoteHistoryCompletelyLoaded = remoteHistoryCompletelyLoaded;
    }

    public String getFirstMamMessageStanzaId() {
        return firstMamMessageStanzaId;
    }

    public void setFirstMamMessageStanzaId(String firstMamMessageStanzaId) {
        this.firstMamMessageStanzaId = firstMamMessageStanzaId;
    }
}
