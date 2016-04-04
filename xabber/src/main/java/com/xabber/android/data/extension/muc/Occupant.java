/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.data.extension.muc;

import android.support.annotation.NonNull;

import com.xabber.android.data.account.StatusMode;

import org.jivesoftware.smackx.muc.MUCAffiliation;
import org.jivesoftware.smackx.muc.MUCRole;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

/**
 * Room occupant.
 *
 * @author alexander.ivanov
 */
public class Occupant implements Comparable<Occupant> {

    private final Resourcepart nickname;

    private Jid jid;

    private MUCRole role;

    private MUCAffiliation affiliation;

    private StatusMode statusMode;

    private String statusText;

    public Occupant(Resourcepart nickname) {
        this.nickname = nickname;
    }

    public Resourcepart getNickname() {
        return nickname;
    }

    /**
     * @return can be <code>null</code>.
     */
    public Jid getJid() {
        return jid;
    }

    public void setJid(Jid jid) {
        this.jid = jid;
    }

    public MUCRole getRole() {
        return role;
    }

    public void setRole(MUCRole role) {
        this.role = role;
    }

    public MUCAffiliation getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(MUCAffiliation affiliation) {
        this.affiliation = affiliation;
    }

    public StatusMode getStatusMode() {
        return statusMode;
    }

    public void setStatusMode(StatusMode statusMode) {
        this.statusMode = statusMode;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    @Override
    public int compareTo(@NonNull Occupant another) {
        int result = another.role.ordinal() - role.ordinal();
        if (result != 0) {
            return result;
        }
        return nickname.toString().compareTo(another.nickname.toString());
    }

}
