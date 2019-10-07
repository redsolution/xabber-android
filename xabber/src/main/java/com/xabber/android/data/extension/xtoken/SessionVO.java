package com.xabber.android.data.extension.xtoken;

public class SessionVO {

    private String client;
    private String device;
    private String uid;
    private String ip;
    private String lastAuth;

    public SessionVO(String client, String device, String uid, String ip, String lastAuth) {
        this.client = client;
        this.device = device;
        this.uid = uid;
        this.ip = ip;
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

    public String getLastAuth() {
        return lastAuth;
    }
}
