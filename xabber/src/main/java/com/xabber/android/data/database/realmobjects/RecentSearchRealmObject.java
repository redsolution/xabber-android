package com.xabber.android.data.database.realmobjects;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RecentSearchRealmObject extends RealmObject {

    private static final String LOG_TAG = RecentSearchRealmObject.class.getSimpleName();

    public static final class Fields{
        public static final String ACCOUNT_JID = "accountJid";
        public static final String CONTACT_JID = "contactJid";
        public static final String TIMESTAMP = "timestamp";
    }

    @PrimaryKey
    private String uuid;
    private String accountJid;
    private String contactJid;
    private Long timestamp;

    public RecentSearchRealmObject(){
        this.uuid = UUID.randomUUID().toString();
    }

    public void setAccountJid(String accountJid) { this.accountJid = accountJid; }
    public String getAccountJid() { return accountJid; }

    public void setContactJid(String contactJid) { this.contactJid = contactJid; }

    public String getContactJid() { return contactJid; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
