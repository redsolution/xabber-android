package com.xabber.android.data.database.realmobjects;

import com.xabber.android.data.roster.ShowOfflineMode;

import io.realm.RealmObject;

public class GroupRealmObject extends RealmObject {

    public static final class Fields {
        public static final String ACCOUNT = "account";
        public static final String GROUP_NAME = "groupName";
        public static final String EXPANDED = "expanded";
        public static final String OFFLINE = "OFFLINE";
    }

    private String account;
    private String groupName;
    private boolean expanded;
    private int offline;

    public GroupRealmObject(String account, String groupName, boolean expanded,
                            ShowOfflineMode showOfflineMode){
        this.account = account;
        this.groupName = groupName;
        this.expanded = expanded;
        this.setShowOfflineMode(showOfflineMode);
    }

    public GroupRealmObject(){
        this.account = "account";
        this.groupName = "group name";
        this.expanded = false;
        this.offline = 0;
    }

    public void setAccount(String account) { this.account = account; }
    public String getAccount() { return account; }

    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getGroupName() { return groupName; }

    public void setExpanded(boolean expanded) { this.expanded = expanded; }
    public boolean isExpanded() { return expanded; }

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
