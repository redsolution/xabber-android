package com.xabber.android.data.entity;


import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xabber.android.data.log.LogManager;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContactJid implements Comparable<ContactJid>, Parcelable {

    private static final String LOG_TAG = ContactJid.class.getSimpleName();

    public static class UserJidCreateException extends IOException {

    }

    private final @NonNull Jid jid;
    private static int counter = 0;
    private static Map<Jid, WeakReference<ContactJid>> instances = new ConcurrentHashMap<>();


    public static @NonNull
    ContactJid from(@Nullable String string) throws UserJidCreateException {
        if (TextUtils.isEmpty(string)) {
            throw new UserJidCreateException();
        }

        Jid jid;
        try {
            jid = JidCreate.from(string);
        } catch (XmppStringprepException e) {
            throw new UserJidCreateException();
        }

        return from(jid);
    }

    public static @NonNull
    ContactJid from(@Nullable Jid jid) throws UserJidCreateException {
        if (jid == null || jid.asBareJid() == null) {
            throw new UserJidCreateException();
        }

        return getUserJid(jid);
    }

    private static ContactJid getUserJid(@NonNull Jid jid) {
        WeakReference<ContactJid> userJidWeakReference = instances.get(jid);

        if (userJidWeakReference != null && userJidWeakReference.get() != null) {
            return userJidWeakReference.get();
        } else {
            ContactJid newContactJid = new ContactJid(jid);
            instances.put(jid, new WeakReference<>(newContactJid));
            return newContactJid;
        }
    }

    private ContactJid(@NonNull Jid jid) {
        this.jid = jid;
        counter++;
    }

    public @NonNull Jid getJid() {
        return jid;
    }

    public @NonNull BareJid getBareJid() {
        return jid.asBareJid();
    }

    public @NonNull
    ContactJid getBareUserJid() {
        return getUserJid(jid.asBareJid());
    }

    @Override
    public int compareTo(@NonNull ContactJid another) {
        return this.getJid().compareTo(another.getJid());
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof ContactJid) {
            return getJid().equals(((ContactJid) o).getJid());
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(jid.toString());
    }

    public static final Parcelable.Creator<ContactJid> CREATOR = new Parcelable.Creator<ContactJid>() {
        @Override
        public ContactJid createFromParcel(Parcel parcel) {
            try {
                return ContactJid.from(parcel.readString());
            } catch (UserJidCreateException e) {
                LogManager.exception(this, e);
                return null;
            }
        }

        @Override
        public ContactJid[] newArray(int size) {
            return new ContactJid[size];
        }
    };
}
