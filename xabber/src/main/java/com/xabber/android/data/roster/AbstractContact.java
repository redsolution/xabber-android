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
package com.xabber.android.data.roster;

import android.graphics.drawable.Drawable;

import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.capability.CapabilitiesManager;
import com.xabber.android.data.extension.capability.ClientInfo;
import com.xabber.android.data.extension.capability.ClientSoftware;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.xmpp.address.Jid;

import org.jivesoftware.smack.packet.Presence;

import java.util.Collection;
import java.util.Collections;


/**
 * Basic contact representation.
 *
 * @author alexander.ivanov
 */
public class AbstractContact extends BaseEntity {


    public AbstractContact(String account, String user) {
        super(account, user);
    }

    /**
     * vCard and roster can be used for name resolving.
     *
     * @return Verbose name.
     */
    public String getName() {
        String vCardName = VCardManager.getInstance().getName(user);

        if (MUCManager.getInstance().isMucPrivateChat(account, user)) {
            String name;

            if (!"".equals(vCardName)) {
                name = vCardName;
            } else {
                name = Jid.getResource(user);
            }

            return String.format("%s (%s)", name, Jid.getBareAddress(user));
        }

        if (!"".equals(vCardName))
            return vCardName;

        return user;
    }

    public StatusMode getStatusMode() {
        return StatusMode.createStatusMode(RosterManager.getInstance().getPresence(account, user));
    }

    public String getStatusText() {
        final String statusText = PresenceManager.getInstance().getStatusText(account, user);
        if (statusText == null) {
            return "";
        } else {
            return statusText;
        }
    }

    public ClientSoftware getClientSoftware() {
        final Presence presence = RosterManager.getInstance().getPresence(account, user);

        if (presence == null || !presence.isAvailable()) {
            return ClientSoftware.unknown;
        }

        final String fullJid = presence.getFrom();
        ClientInfo clientInfo = CapabilitiesManager.getInstance().getClientInfo(account, fullJid);
        if (clientInfo == null) {

            return ClientSoftware.unknown;
        }
        return clientInfo.getClientSoftware();
    }

    public Collection<? extends Group> getGroups() {
        return Collections.emptyList();
    }

    public Drawable getAvatar() {
        return AvatarManager.getInstance().getUserAvatar(user);

    }

    /**
     * @return Cached avatar's drawable for contact list.
     */
    public Drawable getAvatarForContactList() {
        return AvatarManager.getInstance().getUserAvatarForContactList(user);
    }

    /**
     * @return Whether contact is connected.
     */
    public boolean isConnected() {
        return true;
    }

}
