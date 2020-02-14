package com.xabber.android.data.database.realmobjects;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class PhraseNotificationRealm extends RealmObject {

    public static class Fields{
        public static final String ID = "id";
        public static final String VALUE = "value";
        public static final String USER = "user";
        public static final String GROUP = "group";
        public static final String REGEXP = "regexp";
        public static final String SOUND = "sound";
    }

    @PrimaryKey
    private long id;

    private String value;
    private String user;
    private String group;
    private boolean regexp;
    private String sound;

    public PhraseNotificationRealm(long id) { this.id = id; }

    public PhraseNotificationRealm(){ this.id = UUID.randomUUID().toString().toCharArray().hashCode(); }

    public long getId() { return id; }

    public void setValue(String value) { this.value = value; }
    public String getValue() { return value; }

    public void setUser(String user) { this.user = user; }
    public String getUser() { return user; }

    public void setGroup(String group) { this.group = group; }
    public String getGroup() { return group; }

    public void setRegexp(boolean regexp) { this.regexp = regexp; }
    public boolean getRegexp() { return regexp; }

    public void setSound(String sound) { this.sound = sound; }
    public String getSound() { return sound; }

}
