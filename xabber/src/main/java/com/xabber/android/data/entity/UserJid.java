package com.xabber.android.data.entity;


import android.support.annotation.NonNull;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.Serializable;

public class UserJid implements Comparable<UserJid>, Serializable {
    private final @NonNull Jid jid;

    public static UserJid from(@NonNull String string) throws XmppStringprepException {
        return new UserJid(JidCreate.from(string));
    }

    public static UserJid from(@NonNull Jid jid) {
        return new UserJid(jid);
    }

    private UserJid(@NonNull Jid jid) {
        this.jid = jid;
    }

    public @NonNull Jid getJid() {
        return jid;
    }

    @Override
    public int compareTo(@NonNull UserJid another) {
        return this.getJid().compareTo(another.getJid());
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof AccountJid) {
            return getJid().equals(((AccountJid) o).getFullJid());
        } else {
            return false;
        }
    }

    public boolean equals(Jid jid) {
        return jid != null && getJid().equals(jid);
    }

    @Override
    public int hashCode() {
        return getJid().hashCode();
    }

    @Override
    public String toString() {
        return getJid().toString();
    }
}
