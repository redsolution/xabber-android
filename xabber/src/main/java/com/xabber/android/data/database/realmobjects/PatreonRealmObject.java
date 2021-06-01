package com.xabber.android.data.database.realmobjects;

import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Created by valery.miller on 03.10.17.
 */

public class PatreonRealmObject extends RealmObject {

    @PrimaryKey
    @Required
    private String id;

    private String string;
    private int pledged;
    private RealmList<PatreonGoalRealmObject> goals;

    public PatreonRealmObject(String id) {
        this.id = id;
    }

    public PatreonRealmObject() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public int getPledged() {
        return pledged;
    }

    public void setPledged(int pledged) {
        this.pledged = pledged;
    }

    public RealmList<PatreonGoalRealmObject> getGoals() {
        return goals;
    }

    public void setGoals(RealmList<PatreonGoalRealmObject> goals) {
        this.goals = goals;
    }
}
