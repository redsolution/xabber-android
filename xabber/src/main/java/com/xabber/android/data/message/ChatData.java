package com.xabber.android.data.message;

/**
 * Created by valery.miller on 17.10.17.
 */

public class ChatData {

    private String subject;
    private String accountJid;
    private String userJid;
    private int unreadCount;
    private boolean archived;
    private NotificationState notificationState;
    private int lastPosition;

    public ChatData(String subject, String accountJid, String userJid, int unreadCount,
                    boolean archived, NotificationState notificationState, int lastPosition) {
        this.subject = subject;
        this.accountJid = accountJid;
        this.userJid = userJid;
        this.unreadCount = unreadCount;
        this.archived = archived;
        this.notificationState = notificationState;
        this.lastPosition = lastPosition;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getAccountJid() {
        return accountJid;
    }

    public void setAccountJid(String accountJid) {
        this.accountJid = accountJid;
    }

    public String getUserJid() {
        return userJid;
    }

    public void setUserJid(String userJid) {
        this.userJid = userJid;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public NotificationState getNotificationState() {
        return notificationState;
    }

    public void setNotificationState(NotificationState notificationState) {
        this.notificationState = notificationState;
    }

    public int getLastPosition() {
        return lastPosition;
    }

    public void setLastPosition(int lastPosition) {
        this.lastPosition = lastPosition;
    }
}
