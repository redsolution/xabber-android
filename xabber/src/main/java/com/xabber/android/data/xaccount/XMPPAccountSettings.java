package com.xabber.android.data.xaccount;

/**
 * Created by valery.miller on 21.07.17.
 */

public class XMPPAccountSettings {

    private String jid;
    private String username;

    private boolean synchronization;
    private int timestamp;

    private int order;
    private String color;
    private String token;

    public XMPPAccountSettings(String jid, boolean synchronization, int timestamp) {
        this.jid = jid;
        this.synchronization = synchronization;
        this.timestamp = timestamp;
    }

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isSynchronization() {
        return synchronization;
    }

    public void setSynchronization(boolean synchronization) {
        this.synchronization = synchronization;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
