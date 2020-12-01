package com.xabber.android.data.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.xabber.android.data.log.LogManager;

import org.jetbrains.annotations.NotNull;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AccountJid implements Comparable<AccountJid>, Parcelable, Serializable {

    private final @NonNull FullJid fullJid;

    private int order = 0;
    private static final Map<FullJid, AccountJid> instances = new ConcurrentHashMap<>();

    public static AccountJid from(Localpart localpart, DomainBareJid domainBareJid, Resourcepart resource) {
        return getAccountJid(JidCreate.fullFrom(localpart, domainBareJid, resource));
    }

    public static AccountJid from(@NonNull String string) throws XmppStringprepException {
        return getAccountJid(JidCreate.fullFrom(string));
    }

    private AccountJid(@NonNull FullJid fullJid) {
        this.fullJid = fullJid;
    }

    private static AccountJid getAccountJid(@NonNull FullJid fullJid) {
        AccountJid accountJid = instances.get(fullJid);
        if (accountJid != null) {
            return accountJid;
        } else {
            AccountJid newAccountJid = new AccountJid(fullJid);
            instances.put(fullJid, newAccountJid);
            return newAccountJid;
        }
    }

    public @NonNull FullJid getFullJid() {
        return fullJid;
    }

    public BareJid getBareJid() {
        return fullJid.asBareJid();
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int compareTo(@NonNull AccountJid another) {
        return this.order - another.order;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AccountJid) {
            return getFullJid().equals(((AccountJid) o).getFullJid());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getFullJid().hashCode();
    }

    @NotNull
    @Override
    public String toString() {
        return getFullJid().toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fullJid.toString());
    }

    public static final Parcelable.Creator<AccountJid> CREATOR = new Parcelable.Creator<AccountJid>() {
        @Override
        public AccountJid createFromParcel(Parcel parcel) {
            try {
                return AccountJid.from(parcel.readString());
            } catch (XmppStringprepException e) {
                LogManager.exception(this, e);
                return null;
            }
        }

        @Override
        public AccountJid[] newArray(int size) {
            return new AccountJid[size];
        }
    };

}
