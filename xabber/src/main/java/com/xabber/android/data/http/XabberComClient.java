package com.xabber.android.data.http;

import com.xabber.android.data.xaccount.HttpApiManager;

import java.util.List;

import rx.Single;

/**
 * Created by valery.miller on 02.10.17.
 */

public class XabberComClient {

    public static Single<Patreon> getPatreon() {
        return HttpApiManager.getXabberCom().getPatreon();
    }

    public static class Patreon {
        final String string;
        final int pledged;
        final List<PatreonGoal> goals;

        public Patreon(String string, int pledged, List<PatreonGoal> goals) {
            this.string = string;
            this.pledged = pledged;
            this.goals = goals;
        }

        public String getString() {
            return string;
        }

        public int getPledged() {
            return pledged;
        }

        public List<PatreonGoal> getGoals() {
            return goals;
        }
    }

    public static class PatreonGoal {
        final String title;
        final int goal;

        public PatreonGoal(String title, int goal) {
            this.title = title;
            this.goal = goal;
        }

        public String getTitle() {
            return title;
        }

        public int getGoal() {
            return goal;
        }
    }

}
