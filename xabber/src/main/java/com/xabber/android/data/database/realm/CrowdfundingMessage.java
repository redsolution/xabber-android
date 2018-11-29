package com.xabber.android.data.database.realm;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class CrowdfundingMessage extends RealmObject {

    @PrimaryKey
    @Required
    private String id;
    private String timestamp;
    private String messageRu;
    private String messageEn;
    private boolean read;

    public CrowdfundingMessage(String id) {
        this.id = id;
    }

    public CrowdfundingMessage() {
        this.id = UUID.randomUUID().toString();
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageRu() {
        return messageRu;
    }

    public void setMessageRu(String messageRu) {
        this.messageRu = messageRu;
    }

    public String getMessageEn() {
        return messageEn;
    }

    public void setMessageEn(String messageEn) {
        this.messageEn = messageEn;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
