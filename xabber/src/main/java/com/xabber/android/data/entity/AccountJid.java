package com.xabber.android.data.entity;

import android.support.annotation.NonNull;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.Serializable;

public class AccountJid implements Comparable<AccountJid>, Serializable {
    private final @NonNull FullJid fullJid;

    public static AccountJid from(Localpart localpart, DomainBareJid domainBareJid, Resourcepart resource) {
        return new AccountJid(JidCreate.fullFrom(localpart, domainBareJid, resource));
    }

    public static AccountJid from(@NonNull String string) throws XmppStringprepException {
        return new AccountJid(JidCreate.fullFrom(string));
    }

    private AccountJid(@NonNull FullJid fullJid) {
        this.fullJid = fullJid;
    }

    public @NonNull FullJid getFullJid() {
        return fullJid;
    }


    @Override
    public int compareTo(@NonNull AccountJid another) {
        return this.getFullJid().compareTo(another.getFullJid());
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof AccountJid) {
            return getFullJid().equals(((AccountJid) o).getFullJid());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getFullJid().hashCode();
    }

    @Override
    public String toString() {
        return getFullJid().toString();
    }
}
