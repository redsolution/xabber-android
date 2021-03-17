package com.xabber.xmpp.groups;

public class GroupchatPresenceNotificationExtensionElement extends GroupchatExtensionElement {

    private static final String NOTIFICATION_PRESENT = "#present";
    private static final String NOTIFICATION_NOT_PRESENT = "#not-present";

    private boolean isPresent;

    public GroupchatPresenceNotificationExtensionElement(boolean isPresent) {
        this.isPresent = isPresent;
    }

    @Override
    public String getNamespace() {
        if (isPresent) {
            return super.getNamespace() + NOTIFICATION_PRESENT;
        } else return super.getNamespace() + NOTIFICATION_NOT_PRESENT;
    }
}
