package com.xabber.android.data.message;

import com.xabber.android.data.SettingsManager;

/**
 * Created by valery.miller on 21.11.17.
 */

public class NotificationState {

    private NotificationMode mode;
    private long timestamp;

    public NotificationState(NotificationMode mode, long timestamp) {
        this.mode = mode;
        this.timestamp = timestamp;
    }

    public NotificationMode getMode() {
        return mode;
    }
    public void setMode(NotificationMode mode) {
        this.mode = mode;
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Used only for user interface
     */
    public NotificationMode determineModeByGlobalSettings() {
        NotificationState.NotificationMode resultMode = NotificationState.NotificationMode.byDefault;
        boolean globalMode = SettingsManager.eventsOnChat();
        if (mode == NotificationState.NotificationMode.enabled && !globalMode)
            resultMode = NotificationState.NotificationMode.enabled;
        if ((mode != NotificationMode.enabled && mode != NotificationMode.byDefault) && globalMode)
            resultMode = mode;
        return resultMode;
    }

    public enum NotificationMode {
        byDefault,
        enabled,
        disabled,
        snooze15m,
        snooze1h,
        snooze2h,
        snooze1d,
        onlyMentions
    }

}


