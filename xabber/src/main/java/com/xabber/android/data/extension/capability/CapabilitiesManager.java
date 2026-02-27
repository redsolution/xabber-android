/*
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
package com.xabber.android.data.extension.capability;

import android.content.Context;

import androidx.annotation.Nullable;

import com.xabber.android.BuildConfig;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.OnContactChangedListener;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.caps.EntityCapsManager;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provide information about entity capabilities.
 *
 * @author alexander.ivanov
 */
public class CapabilitiesManager {

    private static CapabilitiesManager instance;

    // cache for jids does not supporting Entity Caps
    @SuppressWarnings("WeakerAccess")
    final Map<Jid, DiscoverInfo> discoverInfoCache = new ConcurrentHashMap<>();
    private final Map<Jid, ClientInfo> clientInfoCache = new ConcurrentHashMap<>();

    public static CapabilitiesManager getInstance() {
        if (instance == null) {
            instance = new CapabilitiesManager();
        }

        return instance;
    }

    private CapabilitiesManager() {
        Context applicationContext = Application.getInstance().getApplicationContext();

        EntityCapsManager.setDefaultEntityNode(applicationContext.getString(R.string.caps_entity_node));
        EntityCapsManager.setPersistentCache(new EntityCapsCache());

        setServiceDiscoveryClientIdentity(applicationContext);
    }

    private void setServiceDiscoveryClientIdentity(Context applicationContext) {
        String identityName = applicationContext.getString(R.string.application_title_full)
                + " Android "
                + BuildConfig.VERSION_NAME;
        String identityCategory = "client";
        String identityType = "phone";

        ServiceDiscoveryManager.setDefaultIdentity(
                new DiscoverInfo.Identity(identityCategory, identityName, identityType));
    }

    @Nullable
    public ClientInfo getCachedClientInfo(final Jid jid) {
        ClientInfo clientInfo = clientInfoCache.get(jid);

        if (clientInfo != null) {
            return clientInfo;
        }

        DiscoverInfo discoverInfoByUser = EntityCapsManager.getDiscoverInfoByUser(jid);

        if (discoverInfoByUser == null) {
            discoverInfoByUser = discoverInfoCache.get(jid);
        }

        if (discoverInfoByUser != null) {
            clientInfo =  ClientInfo.fromDiscoveryInfo(discoverInfoByUser);
            clientInfoCache.put(jid, clientInfo);
        }

        return clientInfo;
    }

    public void onPresence(final AccountJid accountJid, final Presence presence) {
        final Jid from = presence.getFrom();

        discoverInfoCache.remove(from);
        clientInfoCache.remove(from);

        DiscoverInfo discoverInfoByUser = EntityCapsManager.getDiscoverInfoByUser(from);
        if (discoverInfoByUser != null) {
            return;
        }

        Application.getInstance().runInBackgroundNetwork(
                () -> updateClientInfo(accountJid, from)
        );
    }

    public void requestClientInfoByUser(final AccountJid account, final Jid jid) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> updateClientInfo(account, jid));
    }

    @SuppressWarnings("WeakerAccess")
    void updateClientInfo(final AccountJid account, final Jid jid) {
        DiscoverInfo discoverInfo = EntityCapsManager.getDiscoverInfoByUser(jid);

        if (discoverInfo != null) {
            return;
        }

        AccountItem accountItem = AccountManager.INSTANCE.getAccount(account);
        if (accountItem == null) {
            return;
        }

        try {
            if (accountItem.isSuccessfulConnectionHappened()) {
                discoverInfo = ServiceDiscoveryManager.getInstanceFor(accountItem.getConnection())
                        .discoverInfo(jid);

                EntityCapsManager.NodeVerHash nodeVerHashByJid = EntityCapsManager.getNodeVerHashByJid(jid);
                if (nodeVerHashByJid == null) {
                    discoverInfoCache.put(jid, discoverInfo);
                }

                if (discoverInfo != null) {
                    clientInfoCache.put(jid, ClientInfo.fromDiscoveryInfo(discoverInfo));
                }
            }
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException | SmackException.NotConnectedException | ClassCastException e) {
            LogManager.exception(this, e);
            clientInfoCache.put(jid, ClientInfo.INVALID_CLIENT_INFO);
        }

        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(account, jid.asBareJid());

        if (rosterContact != null) {
            final ArrayList<RosterContact> rosterContacts = new ArrayList<>();
            rosterContacts.add(rosterContact);

            Application.getInstance().runOnUiThread(() -> {
                for (OnContactChangedListener onContactChangedListener
                        : Application.getInstance().getUIListeners(OnContactChangedListener.class)) {
                    onContactChangedListener.onContactsChanged(rosterContacts);
                }
            });
        }
    }

}
