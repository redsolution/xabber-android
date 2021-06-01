package com.xabber.android.data.database.realmobjects;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Created by valery.miller on 03.10.17.
 */

public class PatreonGoalRealmObject extends RealmObject {

    @PrimaryKey
    @Required
    private String id;

    private String title;
    private int goal;

    public PatreonGoalRealmObject(String id) {
        this.id = id;
    }

    public PatreonGoalRealmObject() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getGoal() {
        return goal;
    }

    public void setGoal(int goal) {
        this.goal = goal;
    }
}
