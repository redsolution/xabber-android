package com.xabber.android.data.database.messagerealm;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class ForwardId extends RealmObject {

    @PrimaryKey
    @Required
    private String id;

    private String forwardMessageId;

    public ForwardId() {
        this.id = UUID.randomUUID().toString();
    }

    public ForwardId(String forwardMessageId) {
        this.id = UUID.randomUUID().toString();
        this.forwardMessageId = forwardMessageId;
    }

    public String getForwardMessageId() {
        return forwardMessageId;
    }

    public void setForwardMessageId(String forwardMessageId) {
        this.forwardMessageId = forwardMessageId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
