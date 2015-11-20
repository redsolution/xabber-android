package com.xabber.android.data.extension.blocking;

import io.realm.RealmObject;

public class BlockedContact extends RealmObject {
    public static final String FIELD_FULL_JID = "fullJid";
    private String fullJid;

    public String getFullJid() {
        return fullJid;
    }

    public void setFullJid(String fullJid) {
        this.fullJid = fullJid;
    }
}
