package com.xabber.android.data.database.realmobjects;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Created by valery.miller on 01.08.17.
 */

public class SocialBindingRealmObject extends RealmObject {

    @PrimaryKey
    @Required
    private String id;

    private String provider;
    private String uid;
    private String firstName;
    private String lastName;

    public SocialBindingRealmObject(String id) {
        this.id = id;
    }

    public SocialBindingRealmObject() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
