package com.xabber.android.data.entity;


import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.xabber.android.data.log.LogManager;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserJid implements Comparable<UserJid>, Parcelable {

    private static final String LOG_TAG = UserJid.class.getSimpleName();

    public static class UserJidCreateException extends IOException {

    }

    private final @NonNull Jid jid;
    private static int counter = 0;
    private static Map<Jid, WeakReference<UserJid>> instances = new ConcurrentHashMap<>();


    public static @NonNull UserJid from(@Nullable String string) throws UserJidCreateException {
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

    public static @NonNull UserJid from(@Nullable Jid jid) throws UserJidCreateException {
        if (jid == null || jid.asBareJid() == null) {
            throw new UserJidCreateException();
        }

        return getUserJid(jid);
    }

    private static UserJid getUserJid(@NonNull Jid jid) {
        WeakReference<UserJid> userJidWeakReference = instances.get(jid);

        if (userJidWeakReference != null && userJidWeakReference.get() != null) {
            return userJidWeakReference.get();
        } else {
            UserJid newUserJid = new UserJid(jid);
            instances.put(jid, new WeakReference<>(newUserJid));
            return newUserJid;
        }
    }

    private UserJid(@NonNull Jid jid) {
        this.jid = jid;
        counter++;
    }

    public @NonNull Jid getJid() {
        return jid;
    }

    public @NonNull BareJid getBareJid() {
        return jid.asBareJid();
    }

    public @NonNull UserJid getBareUserJid() {
        return getUserJid(jid.asBareJid());
    }

    @Override
    public int compareTo(@NonNull UserJid another) {
        return this.getJid().compareTo(another.getJid());
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof UserJid) {
            return getJid().equals(((UserJid) o).getJid());
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

    public static final Parcelable.Creator<UserJid> CREATOR = new Parcelable.Creator<UserJid>() {
        @Override
        public UserJid createFromParcel(Parcel parcel) {
            try {
                return UserJid.from(parcel.readString());
            } catch (UserJidCreateException e) {
                LogManager.exception(this, e);
                return null;
            }
        }

        @Override
        public UserJid[] newArray(int size) {
            return new UserJid[size];
        }
    };
}
