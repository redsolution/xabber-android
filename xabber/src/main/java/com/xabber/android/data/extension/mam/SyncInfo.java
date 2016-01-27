package com.xabber.android.data.extension.mam;

import java.util.Date;

public class SyncInfo {
    private Date lastSyncedTime;

    private String firstMamMessageMamId;
    private String firstMamMessageStanzaId;

    private Integer firstLocalMessagePosition;
    private Date firstLocalMessageTimeStamp;
    private Integer firstMamMessagePosition;

    private String lastMessageMamId;
    private boolean isRemoteHistoryCompletelyLoaded = false;

    public Date getLastSyncedTime() {
        return lastSyncedTime;
    }

    public void setLastSyncedTime(Date lastSyncedTime) {
        this.lastSyncedTime = lastSyncedTime;
    }

    public String getFirstMamMessageMamId() {
        return firstMamMessageMamId;
    }

    public void setFirstMamMessageMamId(String firstMamMessageMamId) {
        this.firstMamMessageMamId = firstMamMessageMamId;
    }

    public Integer getFirstLocalMessagePosition() {
        return firstLocalMessagePosition;
    }

    public void setFirstLocalMessagePosition(Integer firstLocalMessagePosition) {
        this.firstLocalMessagePosition = firstLocalMessagePosition;
    }

    public Date getFirstLocalMessageTimeStamp() {
        return firstLocalMessageTimeStamp;
    }

    public void setFirstLocalMessageTimeStamp(Date firstLocalMessageTimeStamp) {
        this.firstLocalMessageTimeStamp = firstLocalMessageTimeStamp;
    }

    public Integer getFirstMamMessagePosition() {
        return firstMamMessagePosition;
    }

    public void setFirstMamMessagePosition(Integer firstMamMessagePosition) {
        this.firstMamMessagePosition = firstMamMessagePosition;
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

    public String getFirstMamMessageStanzaId() {
        return firstMamMessageStanzaId;
    }

    public void setFirstMamMessageStanzaId(String firstMamMessageStanzaId) {
        this.firstMamMessageStanzaId = firstMamMessageStanzaId;
    }
}
