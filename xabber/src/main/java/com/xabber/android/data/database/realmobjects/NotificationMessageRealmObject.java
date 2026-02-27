package com.xabber.android.data.database.realmobjects;

import androidx.annotation.Nullable;

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
        public static final String MEMBER_ID = "memberId";
    }

    @PrimaryKey
    @Required
    private String id;
    private String author;
    private String text;
    private long timestamp;
    private String memberId;

    public NotificationMessageRealmObject() {
        this.id = UUID.randomUUID().toString();
    }
    public NotificationMessageRealmObject(String id) {
        this.id = id;
    }
    public NotificationMessageRealmObject(String id, String author, String text, long timestamp,
                                          @Nullable String groupMemberId){
        this.id = id;
        this.author = author;
        this.text = text;
        this.timestamp = timestamp;
        this.memberId = groupMemberId;
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

    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getMemberId() { return memberId; }
}
