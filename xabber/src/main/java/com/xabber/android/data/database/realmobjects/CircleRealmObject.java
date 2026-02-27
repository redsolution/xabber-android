package com.xabber.android.data.database.realmobjects;

import com.xabber.android.data.roster.ShowOfflineMode;

import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class CircleRealmObject extends RealmObject {

    public static class Fields {
        public static final String ID = "id";
        public static final String CIRCLE_NAME = "circleName";
        public static final String CONTACT = "contacts";
        public static final String EXPANDED = "expanded";
        public static final String OFFLINE = "offline";
    }

    @PrimaryKey
    @Required
    private String id;

    private String circleName;
    private RealmList<ContactRealmObject> contacts;
    private boolean expanded;
    private int offline;

    public CircleRealmObject() { this.circleName = UUID.randomUUID().toString(); }

    public CircleRealmObject(RealmList<ContactRealmObject> contactRealmObjects, String circleName,
                             boolean expanded, ShowOfflineMode showOfflineMode){
        this.id = UUID.randomUUID().toString();
        this.circleName = circleName;
        this.contacts = contactRealmObjects;
        this.expanded = expanded;
        this.setShowOfflineMode(showOfflineMode);
    }

    public CircleRealmObject(String circleName) {
        this.circleName = circleName;
    }

    public void setCircleName(String circleName) { this.circleName = circleName; }
    public String getCircleName() {
        return circleName;
    }

    public void setExpanded(boolean expanded) { this.expanded = expanded; }
    public boolean isExpanded() { return expanded; }

    public RealmList<ContactRealmObject> getContacts() { return contacts; }

    public ShowOfflineMode getShowOfflineMode() {
        if (offline == -1)
            return ShowOfflineMode.never;
        else if (offline == 0)
            return ShowOfflineMode.normal;
        else if (offline == 1)
            return ShowOfflineMode.always;
        else
            throw new IllegalStateException();
    }

    public void setShowOfflineMode(ShowOfflineMode showOfflineMode){
        switch (showOfflineMode){
            case never: this.offline = -1;
            case normal: this.offline = 0;
            case always: this.offline = 1;
        }
    }
}
