package org.jivesoftware.smackx.mam;

import java.util.Locale;

/**
 * http://xmpp.org/extensions/xep-0313.html#config-default
 */
public enum Behaviour {
    // all messages are archived by default
    always,
    // messages are never archived by default
    never,
    // messages are archived only if the contact's bare JID is in the user's roster.
    roster;

    public static Behaviour fromString(String string) {
        if (string == null) {
            return null;
        }
        return Behaviour.valueOf(string.toLowerCase(Locale.US));
    }
}
