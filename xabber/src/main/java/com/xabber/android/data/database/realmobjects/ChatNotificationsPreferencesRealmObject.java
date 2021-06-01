package com.xabber.android.data.database.realmobjects;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;

public class ChatNotificationsPreferencesRealmObject extends RealmObject {

    public static final class Fields {
        public static final String ID = "id";
        public static final String CHAT = "chatRealmObject";
    }

    @PrimaryKey
    private String id;
    //private RealmResults<ChatRealmObject> chatRealmObject;

    public ChatNotificationsPreferencesRealmObject(){
        this.id = UUID.randomUUID().toString();
    }
}
