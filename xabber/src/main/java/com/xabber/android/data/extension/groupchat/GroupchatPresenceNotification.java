package com.xabber.android.data.extension.groupchat;

public class GroupchatPresenceNotification extends GroupchatExtensionElement {

    private static final String NOTIFICATION_PRESENT = "#present";
    private static final String NOTIFICATION_NOT_PRESENT = "#not-present";

    private boolean isPresent;

    public GroupchatPresenceNotification(boolean isPresent) {
        this.isPresent = isPresent;
    }

    @Override
    public String getNamespace() {
        if (isPresent) return super.getNamespace() + NOTIFICATION_PRESENT;
        else return super.getNamespace() + NOTIFICATION_NOT_PRESENT;
    }
}
