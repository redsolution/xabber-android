package com.xabber.android.data.database.realmobjects;

import io.realm.RealmObject;

public class MessageWithoutReceiptRealmObject extends RealmObject {

    public static final class Fields {
        public static final String MESSAGE_ORIGIN_ID = "messageOriginId";
    }

    private String messageOriginId;

    public String getMessageOriginId() { return messageOriginId; }

    public void setMessageOriginId(String messageOriginId) { this.messageOriginId = messageOriginId; }

    public MessageWithoutReceiptRealmObject(String messageOriginId){
        this.messageOriginId = messageOriginId;
    }

    public MessageWithoutReceiptRealmObject(){ }

}
