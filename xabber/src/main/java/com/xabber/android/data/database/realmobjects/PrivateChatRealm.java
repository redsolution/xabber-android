package com.xabber.android.data.database.realmobjects;

import io.realm.RealmObject;

public class PrivateChatRealm extends RealmObject {

    public static class Fields{
        public static final String ACCOUNT = "account";
        public static final String USER = "user";
    }

    private String account;
    private String user;

    public PrivateChatRealm(String account, String user){
        this.account = account;
        this.user = user;
    }

    public PrivateChatRealm(){
        this.account = "account";
        this.user = "user";
    }

    public void setAccount(String account) { this.account = account; }

    public void setUser(String user) { this.user = user; }

    public String getAccount() { return account; }

    public String getUser() { return user; }
}
