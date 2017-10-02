package com.xabber.android.data.xaccount;

import android.support.annotation.NonNull;

/**
 * Created by valery.miller on 21.07.17.
 */

public class XMPPAccountSettings implements Comparable<XMPPAccountSettings> {

    private String jid;
    private String username;

    private boolean synchronization;
    private int timestamp;

    private int order;
    private String color;
    private String token;
    private boolean syncNotAllowed;

    private boolean deleted;

    private SyncStatus status;

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

    public SyncStatus getStatus() {
        return status;
    }

    public void setStatus(SyncStatus status) {
        this.status = status;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isSyncNotAllowed() {
        return syncNotAllowed;
    }

    public void setSyncNotAllowed(boolean syncNotAllowed) {
        this.syncNotAllowed = syncNotAllowed;
    }

    @Override
    public int compareTo(@NonNull XMPPAccountSettings xmppAccountSettings) {
        return this.getOrder() - xmppAccountSettings.getOrder();
    }

    public enum SyncStatus {

        /**
         * Settings exist only in device
         */
        local,

        /**
         * Settings exist only in remote
         */
        remote,

        /**
         * Settings exist in local and remote
         * Local settings newer than remote
         */
        localNewer,

        /**
         * Settings exist in local and remote
         * Remote settings newer than local
         */
        remoteNewer,

        /**
         * Settings exist in local and remote
         * Both settings has same time
         */
        localEqualsRemote,

        /**
         * Remote settings was deleted
         * Remote settings newer than local
         */
        deleted

    }
}
