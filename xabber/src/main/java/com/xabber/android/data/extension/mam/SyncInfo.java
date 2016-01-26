package com.xabber.android.data.extension.mam;

import java.util.Date;

public class SyncInfo {
    private Date dateLastSynced;
    private String firstMessageId;
    private String lastMessageId;
    private boolean isRemoteHistoryCompletelyLoaded = false;

    public Date getDateLastSynced() {
        return dateLastSynced;
    }

    public void setDateLastSynced(Date dateLastSynced) {
        this.dateLastSynced = dateLastSynced;
    }

    public String getFirstMessageId() {
        return firstMessageId;
    }

    public void setFirstMessageId(String firstMessageId) {
        this.firstMessageId = firstMessageId;
    }

    public String getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(String lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public boolean isRemoteHistoryCompletelyLoaded() {
        return isRemoteHistoryCompletelyLoaded;
    }

    public void setRemoteHistoryCompletelyLoaded(boolean remoteHistoryCompletelyLoaded) {
        isRemoteHistoryCompletelyLoaded = remoteHistoryCompletelyLoaded;
    }
}
