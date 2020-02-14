package com.xabber.android.data.database.realmobjects;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class AvatarRealm extends RealmObject {

    public static final class Fields{
        public static final String USER = "user";
        public static final String HASH = "hash";
        public static final String PEP_HASH = "pepHash";
    }

    @PrimaryKey
    private String user;

    private String hash;
    private String pepHash;

    public AvatarRealm(String user){ this.user = user; }

    public AvatarRealm(){this.user = "user"; }

    public void setUser(String user) { this.user = user; }
    public String getUser() { return user; }

    public void setHash(String hash) { this.hash = hash; }
    public String getHash() { return hash; }

    public void setPepHash(String pepHash) { this.pepHash = pepHash; }
    public String getPepHash() { return pepHash; }
}
