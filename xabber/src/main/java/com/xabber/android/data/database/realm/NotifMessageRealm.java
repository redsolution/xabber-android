package com.xabber.android.data.database.realm;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class NotifMessageRealm extends RealmObject {

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

    public NotifMessageRealm() {
        this.id = UUID.randomUUID().toString();
    }

    public NotifMessageRealm(String id) {
        this.id = id;
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
