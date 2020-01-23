package com.xabber.android.data.database.messagerealm;

import android.os.Looper;

import com.xabber.android.data.extension.references.Forward;
import com.xabber.android.data.log.LogManager;

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
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(ForwardId.class.getSimpleName(), new IllegalStateException("Tried to read from non UI")); 
        return forwardMessageId;
    }

    public void setForwardMessageId(String forwardMessageId) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(ForwardId.class.getSimpleName(), new IllegalStateException("Tried to write from UI!")); 
        this.forwardMessageId = forwardMessageId;
    }

    public String getId() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(ForwardId.class.getSimpleName(), new IllegalStateException("Tried to read from non UI")); 
        return id;
    }

    public void setId(String id) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(ForwardId.class.getSimpleName(), new IllegalStateException("Tried to write from UI!")); 
        this.id = id;
    }
}
