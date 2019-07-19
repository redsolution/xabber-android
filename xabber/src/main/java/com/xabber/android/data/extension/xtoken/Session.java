package com.xabber.android.data.extension.xtoken;

public class Session {

    public static final String ELEMENT = "field";
    public static final String ELEMENT_CLIENT = "client";
    public static final String ELEMENT_DEVICE = "device";
    public static final String ELEMENT_TOKEN_UID = "token-uid";
    public static final String ELEMENT_EXPIRE = "expire";
    public static final String ELEMENT_IP = "ip";
    public static final String ELEMENT_LAST_AUTH= "last-auth";

    private String client;
    private String device;
    private String uid;
    private String ip;
    private long expire;
    private long lastAuth;

    public Session(String client, String device, String uid, String ip, long expire, long lastAuth) {
        this.client = client;
        this.device = device;
        this.uid = uid;
        this.ip = ip;
        this.expire = expire;
        this.lastAuth = lastAuth;
    }

    public String getClient() {
        return client;
    }

    public String getDevice() {
        return device;
    }

    public String getUid() {
        return uid;
    }

    public String getIp() {
        return ip;
    }

    public long getExpire() {
        return expire;
    }

    public long getLastAuth() {
        return lastAuth;
    }
}
