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
    private boolean muted;

    public ChatData(String subject, String accountJid, String userJid, int unreadCount, boolean archived, boolean muted) {
        this.subject = subject;
        this.accountJid = accountJid;
        this.userJid = userJid;
        this.unreadCount = unreadCount;
        this.archived = archived;
        this.muted = muted;
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

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }
}
