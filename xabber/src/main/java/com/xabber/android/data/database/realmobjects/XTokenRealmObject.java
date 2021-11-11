package com.xabber.android.data.database.realmobjects;

import com.xabber.android.data.extension.xtoken.XToken;
import com.xabber.android.data.log.LogManager;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class XTokenRealmObject extends RealmObject {

    public static class Fields {
        public static final String ID = "id";
    }

    @PrimaryKey
    @Required
    private String id;
    private String token;
    private long expire;
    private int counter;

    public XTokenRealmObject() {
        this.id = UUID.randomUUID().toString();
    }

    public XTokenRealmObject(String id, String token, long expire, int counter) {
        this.id = id;
        this.token = token;
        this.expire = expire;
        this.counter = counter;
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

    public int getCounter() { return counter; }

    public void setCounter(int counter) { this.counter = counter; }

    static public XTokenRealmObject createFromXToken(XToken xToken) {
        return new XTokenRealmObject(
                xToken.getUid(), xToken.getToken(), xToken.getExpire(), xToken.getCounter()
        );
    }

    public XToken toXToken() {
        return new XToken(id, token, expire, counter);
    }

}
