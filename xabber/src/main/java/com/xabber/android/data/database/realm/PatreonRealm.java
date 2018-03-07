package com.xabber.android.data.database.realm;

import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Created by valery.miller on 03.10.17.
 */

public class PatreonRealm extends RealmObject {

    @PrimaryKey
    @Required
    private String id;

    private String string;
    private int pledged;
    private RealmList<PatreonGoalRealm> goals;

    public PatreonRealm(String id) {
        this.id = id;
    }

    public PatreonRealm() {
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

    public RealmList<PatreonGoalRealm> getGoals() {
        return goals;
    }

    public void setGoals(RealmList<PatreonGoalRealm> goals) {
        this.goals = goals;
    }
}
