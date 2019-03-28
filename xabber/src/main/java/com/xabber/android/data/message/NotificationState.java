package com.xabber.android.data.message;

import com.xabber.android.data.SettingsManager;

/**
 * Created by valery.miller on 21.11.17.
 */

public class NotificationState {

    public enum NotificationMode {
        bydefault,
        enabled,
        disabled,
        snooze15m,
        snooze1h,
        snooze2h,
        snooze1d
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

    /** Used only for user interface */
    public NotificationMode determineModeByGlobalSettings(boolean isMUC) {
        NotificationState.NotificationMode resultMode = NotificationState.NotificationMode.bydefault;
        boolean globalMode = isMUC ? SettingsManager.eventsOnMuc() : SettingsManager.eventsOnChat();
        if (mode == NotificationState.NotificationMode.enabled && !globalMode)
            resultMode = NotificationState.NotificationMode.enabled;
        if ((mode != NotificationMode.enabled && mode != NotificationMode.bydefault) && globalMode)
            resultMode = mode;
        return resultMode;
    }

}


