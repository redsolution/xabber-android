package com.xabber.android.data.database.realm;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class ContactGroup extends RealmObject {

    public static class Fields {
        public static final String GROUP_NAME = "groupName";
    }

    @PrimaryKey
    @Required
    private String groupName;

    public ContactGroup() {
        this.groupName = UUID.randomUUID().toString();
    }

    public ContactGroup(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupName() {
        return groupName;
    }
}
