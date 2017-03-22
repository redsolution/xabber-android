package com.xabber.android.data.roster;

import org.jivesoftware.smack.packet.Presence;

import java.util.Comparator;

class PresenceComparatorByPriority implements Comparator<Presence> {
    static PresenceComparatorByPriority INSTANCE = new PresenceComparatorByPriority();

    @Override
    public int compare(Presence presence1, Presence presence2) {
        int priority1 = presence1.getPriority();
        int priority2 = presence2.getPriority();

        if (priority1 == Integer.MIN_VALUE) {
            return 1;
        }

        if (priority2 == Integer.MIN_VALUE) {
            return -1;
        }

        return priority2 - priority1;
    }
}
