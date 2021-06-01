package com.xabber.android.data.database.realmobjects;

import io.realm.RealmObject;

public class OtrRealmObject extends RealmObject {
    public static final class Fields{
        public static final String ACCOUNT = "account";
        public static final String USER = "user";
        public static final String FINGERPRINT = "fingerprint";
        public static final String VERIFIED = "verified";
    }

    private String account;
    private String user;
    private String fingerprint;
    private boolean verified;

    public OtrRealmObject(String account, String user, String fingerprint, boolean verified){
        this.account = account;
        this.user = user;
        this.fingerprint = fingerprint;
        this.verified = verified;
    }

    public OtrRealmObject(){
        this.account = "account";
        this.user = "user";
        this.fingerprint = "fingerprint";
        this.verified = false;
    }

    public void setAccount(String account) { this.account = account; }
    public String getAccount() { return account; }

    public void setUser(String user) { this.user = user; }
    public String getUser() { return user; }

    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    public String getFingerprint() { return fingerprint; }

    public void setVerified(boolean verified) { this.verified = verified; }
    public boolean isVerified() { return verified; }
}
