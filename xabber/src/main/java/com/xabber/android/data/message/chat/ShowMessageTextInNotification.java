package com.xabber.android.data.message.chat;


public enum ShowMessageTextInNotification {

    /**
     * Show message text in notifications according to global settings.
     */
    default_settings,

    /**
     * Always show message text in notifications.
     */
    show,

    /**
     * Never show message text in notifications.
     */
    hide;

    public static ShowMessageTextInNotification fromInteger(int x) {
        switch (x) {
            case 0:
                return default_settings;
            case 1:
                return show;
            case 2:
                return hide;
            default:
                return default_settings;
        }
    }

}
