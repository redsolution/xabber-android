package com.xabber.android.data.notification.custom_notification;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;

import org.jxmpp.stringprep.XmppStringprepException;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class NotifyPrefsRealm extends RealmObject {

    public static class Fields {
        public static final String ID = "id";
        public static final String ACCOUNT = "account";
        public static final String USER = "user";
        public static final String CHANNEL_ID = "channelID";
        public static final String TYPE = "type";
        public static final String GROUP = "group";
        public static final String PHRASE_ID = "phraseID";
        public static final String VIBRO = "vibro";
        public static final String SHOW_PREVIEW = "showPreview";
        public static final String SOUND = "sound";
    }

    @PrimaryKey
    @Required
    private String id;

    private String channelID;
    private String type;

    private String account;
    private String user;
    private String group;
    private Long phraseID;

    private String vibro;
    private boolean showPreview;
    private String sound;

    public NotifyPrefsRealm(String id) {
        this.id = id;
    }

    public NotifyPrefsRealm() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public String getChannelID() {
        return channelID;
    }

    public void setChannelID(String channelID) {
        this.channelID = channelID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public AccountJid getAccount() {
        if (account == null) return null;
        try {
            return AccountJid.from(account);
        } catch (XmppStringprepException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
    }

    public void setAccount(AccountJid account) {
        this.account = account.toString();
    }

    public UserJid getUser() {
        if (user == null) return null;
        try {
            return UserJid.from(user);
        } catch (UserJid.UserJidCreateException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
    }

    public void setUser(UserJid user) {
        this.user = user.toString();
    }


    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Long getPhraseID() {
        return phraseID;
    }

    public void setPhraseID(Long phraseID) {
        this.phraseID = phraseID;
    }

    public String getVibro() {
        return vibro;
    }

    public void setVibro(String vibro) {
        this.vibro = vibro;
    }

    public boolean isShowPreview() {
        return showPreview;
    }

    public void setShowPreview(boolean showPreview) {
        this.showPreview = showPreview;
    }

    public String getSound() {
        return sound;
    }

    public void setSound(String sound) {
        this.sound = sound;
    }
}
