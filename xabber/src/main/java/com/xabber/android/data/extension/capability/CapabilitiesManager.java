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
package com.xabber.android.data.extension.capability;

import android.content.Context;
import android.support.annotation.Nullable;

import com.xabber.android.BuildConfig;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.caps.EntityCapsManager;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provide information about entity capabilities.
 *
 * @author alexander.ivanov
 */
public class CapabilitiesManager {

    public static final ClientInfo INVALID_CLIENT_INFO = new ClientInfo(null,
            null, null, new ArrayList<String>());
    @SuppressWarnings("WeakerAccess")
    static final String LOG_TAG = CapabilitiesManager.class.getSimpleName();

    private static CapabilitiesManager instance;

    @SuppressWarnings("WeakerAccess")
    Map<Jid, DiscoverInfo> discoverInfoCache;

    public static CapabilitiesManager getInstance() {
        if (instance == null) {
            instance = new CapabilitiesManager();
        }

        return instance;
    }

    private CapabilitiesManager() {
        Context applicationContext = Application.getInstance().getApplicationContext();

        EntityCapsManager.setDefaultEntityNode(applicationContext.getString(R.string.caps_entity_node));


        setServiceDiscoveryClientIdentity(applicationContext);

        discoverInfoCache = new ConcurrentHashMap<>();
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

    private static Collection<String> getFeatures(DiscoverInfo discoverInfo) {
        Collection<String> features = new ArrayList<>();
        for (DiscoverInfo.Feature feature : discoverInfo.getFeatures()) {
            features.add(feature.getVar());
        }
        return features;
    }

    public void updateClientInfo(final AccountJid accountJid, final Presence presence) {
        DiscoverInfo discoverInfoByUser = EntityCapsManager.getDiscoverInfoByUser(presence.getFrom());
        if (discoverInfoByUser != null) {
            return;
        }

        discoverInfoCache.remove(presence.getFrom());

        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                updateClientInfo(accountJid, presence.getFrom());
            }
        });
    }

    @Nullable
    public ClientInfo getCachedClientInfo(final Jid jid) {
        DiscoverInfo discoverInfoByUser = EntityCapsManager.getDiscoverInfoByUser(jid);

        if (discoverInfoByUser == null) {
            discoverInfoByUser = discoverInfoCache.get(jid);
        }

        if (discoverInfoByUser != null) {
            return getClientInfo(discoverInfoByUser);
        }

        return null;
    }

    public ClientInfo getClientInfo(final AccountJid account, final Jid jid) {
        DiscoverInfo discoverInfoByUser = EntityCapsManager.getDiscoverInfoByUser(jid);

        if (discoverInfoByUser == null) {
            discoverInfoByUser = discoverInfoCache.get(jid);
            if (discoverInfoByUser != null) {
                LogManager.i(LOG_TAG, "found discover info for user in local cache : " + jid);
            }
        }

        if (discoverInfoByUser != null) {
            return getClientInfo(discoverInfoByUser);
        }

        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                updateClientInfo(account, jid);
            }
        });

        return null;
    }

    @SuppressWarnings("WeakerAccess")
    void updateClientInfo(final AccountJid account, final Jid jid) {
        DiscoverInfo discoverInfo = EntityCapsManager.getDiscoverInfoByUser(jid);

        if (discoverInfo != null) {
            return;
        }

        try {
            discoverInfo = ServiceDiscoveryManager.getInstanceFor(AccountManager.getInstance().getAccount(account).getConnection())
                    .discoverInfo(jid);

            EntityCapsManager.NodeVerHash nodeVerHashByJid = EntityCapsManager.getNodeVerHashByJid(jid);
            if (nodeVerHashByJid == null) {
                discoverInfoCache.put(jid, discoverInfo);
            }

            RosterContact rosterContact = RosterManager.getInstance().getRosterContact(account, jid.asBareJid());

            if (rosterContact != null) {
                final ArrayList<RosterContact> rosterContacts = new ArrayList<>();
                rosterContacts.add(rosterContact);

                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (OnContactChangedListener onContactChangedListener
                                : Application.getInstance().getUIListeners(OnContactChangedListener.class)) {
                            onContactChangedListener.onContactsChanged(rosterContacts);
                        }
                    }
                });
            }


        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException | SmackException.NotConnectedException e) {
            LogManager.exception(this, e);
        }
    }


    private static ClientInfo getClientInfo(DiscoverInfo discoverInfo) {
        if (discoverInfo == null) {
            return null;
        }

        for (int useClient = 1; useClient >= 0; useClient--) {
            for (int useLanguage = 2; useLanguage >= 0; useLanguage--) {
                for (DiscoverInfo.Identity identity : discoverInfo.getIdentities()) {
                    if (useClient == 1 && !"client".equals(identity.getCategory())) {
                        continue;
                    }
                    if (useLanguage == 2 && !Stanza.getDefaultLanguage().equals(identity.getLanguage())) {
                        continue;
                    }
                    if (useLanguage == 1 && identity.getLanguage() != null) {
                        continue;
                    }
                    return new ClientInfo(identity.getType(), identity.getName(),
                            discoverInfo.getNode(), getFeatures(discoverInfo));
                }
            }
        }
        return new ClientInfo(null, null, null, getFeatures(discoverInfo));
    }


}
