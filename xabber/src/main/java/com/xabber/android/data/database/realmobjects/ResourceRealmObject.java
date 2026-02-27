package com.xabber.android.data.database.realmobjects;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class ResourceRealmObject extends RealmObject {
    public static final class Fields{
        public static final String ID = "id";
        public static final String CONTACT = "contact";
        public static final String RESOURCE = "resource";
        public static final String RESOURCE_PRIORITY = "resourcePriority";
        public static final String STATUS = "status";
        public static final String PRESENCE = "presence";
    }

    @PrimaryKey
    private String id;

    public ResourceRealmObject(){
        this.id = UUID.randomUUID().toString();
    }

}
