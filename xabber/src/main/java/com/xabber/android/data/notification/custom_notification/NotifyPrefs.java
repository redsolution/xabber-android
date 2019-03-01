package com.xabber.android.data.notification.custom_notification;

public class NotifyPrefs {

    private String id;
    private String channelID;
    private Key key;

    // notification preferences
    private String vibro;
    private boolean showPreview;
    private String sound;

    public NotifyPrefs(String id, Key key, String vibro, boolean showPreview, String sound) {
        this.id = id;
        this.key = key;
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

    public Key getKey() {
        return key;
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
