package com.xabber.android.data.entity;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.io.Serializable;

public class UserJid implements Comparable<UserJid>, Serializable {

    public static class UserJidCreateException extends IOException {

    }

    private final @NonNull Jid jid;

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

        return new UserJid(jid);
    }

    private UserJid(@NonNull Jid jid) {
        this.jid = jid;
    }

    public @NonNull Jid getJid() {
        return jid;
    }

    public @NonNull BareJid getBareJid() {
        return jid.asBareJid();
    }

    public @NonNull UserJid getBareUserJid() {
        return new UserJid(jid.asBareJid());
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
}
