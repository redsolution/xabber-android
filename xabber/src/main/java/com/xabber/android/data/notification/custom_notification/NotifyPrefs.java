package com.xabber.android.data.notification.custom_notification;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;

public class NotifyPrefs {

    private String id;
    private String channelID;
    private Type type;

    // address
    private AccountJid account;
    private UserJid user;
    private String group;
    private Long phraseID;

    // notification preferences
    private String vibro;
    private boolean showPreview;
    private String sound;

    public NotifyPrefs(String id, Type type, AccountJid account, UserJid user, String group,
                       Long phraseID, String vibro, boolean showPreview, String sound) {
        this.id = id;
        this.type = type;
        this.account = account;
        this.user = user;
        this.group = group;
        this.phraseID = phraseID;
        this.vibro = vibro;
        this.showPreview = showPreview;
        this.sound = sound;
    }

    public String getId() {
        return id;
    }

    public String getChannelID() {
        return channelID;
    }

    public Type getType() {
        return type;
    }

    public AccountJid getAccount() {
        return account;
    }

    public UserJid getUser() {
        return user;
    }

    public String getGroup() {
        return group;
    }

    public Long getPhraseID() {
        return phraseID;
    }

    public String getVibro() {
        return vibro;
    }

    public boolean isShowPreview() {
        return showPreview;
    }

    public String getSound() {
        return sound;
    }

    public void setVibro(String vibro) {
        this.vibro = vibro;
    }

    public void setShowPreview(boolean showPreview) {
        this.showPreview = showPreview;
    }

    public void setSound(String sound) {
        this.sound = sound;
    }

    public void setChannelID(String channelID) {
        this.channelID = channelID;
    }

    public enum Type {
        phrase,
        chat,
        group,
        account;

        public static Type get(String type) {
            switch (type) {
                case "phrase": return phrase;
                case "group": return group;
                case "account": return account;
                default: return chat;
            }
        }
    }

}
