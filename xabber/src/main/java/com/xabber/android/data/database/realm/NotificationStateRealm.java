package com.xabber.android.data.database.realm;

import com.xabber.android.data.message.NotificationState;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Created by valery.miller on 21.11.17.
 */

public class NotificationStateRealm extends RealmObject {

    @PrimaryKey
    @Required
    private String id;

    private String mode;
    private int timestamp;

    public NotificationStateRealm() {
        this.id = UUID.randomUUID().toString();
    }

    public NotificationState.NotificationMode getMode() {
        return (mode != null) ? NotificationState.NotificationMode.valueOf(mode)
                : NotificationState.NotificationMode.bydefault;
    }

    public void setMode(NotificationState.NotificationMode mode) {
        this.mode = mode.toString();
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }
}
