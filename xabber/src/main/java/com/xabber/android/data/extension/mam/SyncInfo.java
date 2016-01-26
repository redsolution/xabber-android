package com.xabber.android.data.extension.mam;

import java.util.Date;

public class SyncInfo {
    private Date lastSyncedTime;
    private String firstMessageMamId;
    private String lastMessageMamId;
    private boolean isRemoteHistoryCompletelyLoaded = false;

    public Date getLastSyncedTime() {
        return lastSyncedTime;
    }

    public void setLastSyncedTime(Date lastSyncedTime) {
        this.lastSyncedTime = lastSyncedTime;
    }

    public String getFirstMessageMamId() {
        return firstMessageMamId;
    }

    public void setFirstMessageMamId(String firstMessageMamId) {
        this.firstMessageMamId = firstMessageMamId;
    }

    public String getLastMessageMamId() {
        return lastMessageMamId;
    }

    public void setLastMessageMamId(String lastMessageMamId) {
        this.lastMessageMamId = lastMessageMamId;
    }

    public boolean isRemoteHistoryCompletelyLoaded() {
        return isRemoteHistoryCompletelyLoaded;
    }

    public void setRemoteHistoryCompletelyLoaded(boolean remoteHistoryCompletelyLoaded) {
        isRemoteHistoryCompletelyLoaded = remoteHistoryCompletelyLoaded;
    }
}
