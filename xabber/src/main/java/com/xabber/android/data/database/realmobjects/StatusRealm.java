package com.xabber.android.data.database.realmobjects;

import io.realm.RealmObject;

public class StatusRealm extends RealmObject {

    public static class Fields{
        public static final String STATUS_MODE = "statusMode";
        public static final String STATUS_TEXT = "statusText";
    }

    private String statusMode;
    private String statusText;

    public StatusRealm(String statusMode, String statusText){
        this.statusMode = statusMode;
        this.statusText = statusText;
    }

    public StatusRealm(){
        this.statusMode = "unavailable";
        this.statusText = "Saved text";
    }

    public void setStatusMode(String statusMode) { this.statusMode = statusMode; }

    public void setStatusText(String statusText) { this.statusText = statusText; }

    public String getStatusMode() { return statusMode; }

    public String getStatusText() { return statusText; }
}
