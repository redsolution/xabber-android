package com.xabber.android.data.database.realm;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Created by valery.miller on 17.10.17.
 */

public class ChatDataRealm extends RealmObject {

    @PrimaryKey
    @Required
    private String id;

    private String subject;
    private String accountJid;
    private String userJid;
    private int unreadCount;
    private boolean archived;
    private NotificationStateRealm notificationState;
    private int lastPosition;

    public ChatDataRealm(String accountJid, String userJid) {
        this.id = accountJid + "-" + userJid;
        this.accountJid = accountJid;
        this.userJid = userJid;
    }

    public ChatDataRealm() {
        this.id = UUID.randomUUID().toString();
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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public NotificationStateRealm getNotificationState() {
        return notificationState;
    }

    public void setNotificationState(NotificationStateRealm notificationState) {
        this.notificationState = notificationState;
    }

    public int getLastPosition() {
        return lastPosition;
    }

    public void setLastPosition(int lastPosition) {
        this.lastPosition = lastPosition;
    }
}
