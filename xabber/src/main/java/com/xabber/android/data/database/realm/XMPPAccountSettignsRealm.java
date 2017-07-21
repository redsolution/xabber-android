package com.xabber.android.data.database.realm;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Created by valery.miller on 21.07.17.
 */

public class XMPPAccountSettignsRealm extends RealmObject {

    @PrimaryKey
    @Required
    private String jid;
    private String username;

    private boolean synchronization;
    private int timestamp;

    private int order;
    private String color;
    private String token;

    public XMPPAccountSettignsRealm(String jid) {
        this.jid = jid;
    }
    public XMPPAccountSettignsRealm() {
        this.jid = UUID.randomUUID().toString();
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
