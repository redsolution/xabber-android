package com.xabber.android.data.database.realmobjects;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class PushLogRecordRealmObject extends RealmObject {

    public static class Fields {
        public static final String ID = "id";
        public static final String TIME = "time";
        public static final String MESSAGE = "message";
    }

    @PrimaryKey
    @Required
    private String id;
    private long time;
    private String message;

    public PushLogRecordRealmObject(long time, String message) {
        this.id = UUID.randomUUID().toString();
        this.time = time;
        this.message = message;
    }

    public PushLogRecordRealmObject() {
        this.id = UUID.randomUUID().toString();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
