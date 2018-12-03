package com.xabber.android.data.database.realm;

import com.xabber.android.data.Application;

import java.util.Locale;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class CrowdfundingMessage extends RealmObject {

    @PrimaryKey
    @Required
    private String id;
    private int timestamp;
    private String messageRu;
    private String messageEn;
    private boolean read;

    public CrowdfundingMessage(String id) {
        this.id = id;
    }

    public CrowdfundingMessage() {
        this.id = UUID.randomUUID().toString();
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
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

    public String getMessageForCurrentLocale() {
        Locale currentLocale = Application.getInstance().getResources().getConfiguration().locale;
        if (currentLocale.getLanguage().equals("ru"))
            return getMessageRu();
        else return getMessageEn();
    }
}
