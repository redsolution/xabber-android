package com.xabber.android.data.database.realm;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class XTokenRealm extends RealmObject {

    public static class Fields {
        public static final String ID = "id";
        public static final String TOKEN = "token";
        public static final String EXPIRE = "expire";
    }

    @PrimaryKey
    @Required
    private String id;
    private String token;
    private long expire;

    public XTokenRealm() {
        this.id = UUID.randomUUID().toString();
    }

    public XTokenRealm(String id, String token, long expire) {
        this.id = id;
        this.token = token;
        this.expire = expire;
    }

    public String getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }
}
