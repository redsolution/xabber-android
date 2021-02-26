package com.xabber.android.data.database.realmobjects;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class NotificationMessageRealmObject extends RealmObject {

    public static class Fields {
        public static final String ID = "id";
        public static final String AUTHOR = "author";
        public static final String TEXT = "text";
        public static final String TIMESTAMP = "timestamp";
    }

    @PrimaryKey
    @Required
    private String id;
    private String author;
    private String text;
    private long timestamp;

    public NotificationMessageRealmObject() {
        this.id = UUID.randomUUID().toString();
    }

    public NotificationMessageRealmObject(String id) {
        this.id = id;
    }

    public NotificationMessageRealmObject(String id, String author, String text, long timestamp){
        this.id = id;
        this.author = author;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
