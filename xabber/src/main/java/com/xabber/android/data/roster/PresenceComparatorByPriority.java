package com.xabber.android.data.roster;

import org.jivesoftware.smack.packet.Presence;

import java.util.Comparator;

class PresenceComparatorByPriority implements Comparator<Presence> {
    static PresenceComparatorByPriority INSTANCE = new PresenceComparatorByPriority();

    @Override
    public int compare(Presence presence1, Presence presence2) {
        int priority1 = presence1.getPriority();
        int priority2 = presence2.getPriority();
        if (priority1 > priority2) {
            return 1;
        } else if (priority2 > priority1) {
            return -1;
        } else return 0;
    }
}
