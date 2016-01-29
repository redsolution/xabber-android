package com.xabber.android.data.extension.mam;

import java.util.Date;

public class SyncCache {
    private Date lastSyncedTime;
    private Integer firstLocalMessagePosition;
    private Date firstLocalMessageTimeStamp;
    private Integer firstMamMessagePosition;

    public Date getLastSyncedTime() {
        return lastSyncedTime;
    }

    public void setLastSyncedTime(Date lastSyncedTime) {
        this.lastSyncedTime = lastSyncedTime;
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
}
