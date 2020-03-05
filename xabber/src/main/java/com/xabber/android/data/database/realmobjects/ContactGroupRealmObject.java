package com.xabber.android.data.database.realmobjects;

import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class ContactGroupRealmObject extends RealmObject {

    public static class Fields {
        public static final String GROUP_NAME = "groupName";
        public static final String CONTACT = "contacts";
    }

    @PrimaryKey
    @Required
    private String groupName;
    private RealmList<ContactRealmObject> contacts;

    public ContactGroupRealmObject() {
        this.groupName = UUID.randomUUID().toString();
    }

    public ContactGroupRealmObject(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupName() {
        return groupName;
    }

    public RealmList<ContactRealmObject> getContacts() { return contacts; }
}
