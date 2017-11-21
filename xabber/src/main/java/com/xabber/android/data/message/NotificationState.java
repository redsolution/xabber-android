package com.xabber.android.data.message;

/**
 * Created by valery.miller on 21.11.17.
 */

public class NotificationState {

    public enum NotificationMode {
        bydefault,
        enabled,
        disabled,
        disabled1h,
        disabled8h,
        disabled2d
    }

    private NotificationMode mode;
    private int timestamp;

    public NotificationState(NotificationMode mode, int timestamp) {
        this.mode = mode;
        this.timestamp = timestamp;
    }

    public NotificationMode getMode() {
        return mode;
    }

    public void setMode(NotificationMode mode) {
        this.mode = mode;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

}


