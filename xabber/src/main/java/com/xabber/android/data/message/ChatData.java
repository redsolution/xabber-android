package com.xabber.android.data.message;

/**
 * Created by valery.miller on 17.10.17.
 */

public class ChatData {

    private String accountJid;
    private String userJid;
    private boolean archived;
    private NotificationState notificationState;
    private int lastPosition;
    private boolean historyRequestedAtStart;
    private int state;
    private boolean isGroupchat;

    public ChatData(String accountJid, String userJid,
                    boolean archived, NotificationState notificationState, int lastPosition,
                    boolean historyRequestedAtStart, int state,
                    boolean isGroupchat) {
        this.accountJid = accountJid;
        this.userJid = userJid;
        this.archived = archived;
        this.notificationState = notificationState;
        this.lastPosition = lastPosition;
        this.historyRequestedAtStart = historyRequestedAtStart;
        this.state = state;
        this.isGroupchat = isGroupchat;
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

    public boolean isHistoryRequestedAtStart() {
        return historyRequestedAtStart;
    }

    public int getLastState() {
        return state;
    }

    public void setLastState(int state) {
        this.state = state;
    }

    public boolean isGroupchat() {
        return isGroupchat;
    }

    public void setGroupchat(boolean groupchat) {
        isGroupchat = groupchat;
    }
}
