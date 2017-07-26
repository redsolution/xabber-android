package com.xabber.android.data.database.realm;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Created by valery.miller on 26.07.17.
 */

public class EmailRealm extends RealmObject {

    @PrimaryKey
    @Required
    private String id;

    private String email;
    private boolean verified;
    private boolean primary;

    public EmailRealm(String id) {
        this.id = id;
    }

    public EmailRealm() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}
