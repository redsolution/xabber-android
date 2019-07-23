package com.xabber.android.data.extension.xtoken;

public class XToken {

    private String uid;
    private String token;
    private long expire;

    public XToken(String uid, String token, long expire) {
        this.uid = uid;
        this.token = token;
        this.expire = expire;
    }

    public String getUid() {
        return uid;
    }

    public String getToken() {
        return token;
    }

    public long getExpire() {
        return expire;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expire;
    }
}
