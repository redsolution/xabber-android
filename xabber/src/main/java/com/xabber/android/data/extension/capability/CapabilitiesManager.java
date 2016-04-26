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

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.caps.EntityCapsManager;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Provide information about entity capabilities.
 *
 * @author alexander.ivanov
 */
public class CapabilitiesManager {

    public static final ClientInfo INVALID_CLIENT_INFO = new ClientInfo(null,
            null, null, new ArrayList<String>());

    private final static CapabilitiesManager instance;

    static {
        instance = new CapabilitiesManager();
        Application.getInstance().addManager(instance);
    }

    private static Collection<String> getFeatures(DiscoverInfo discoverInfo) {
        Collection<String> features = new ArrayList<>();
        for (DiscoverInfo.Feature feature : discoverInfo.getFeatures()) {
            features.add(feature.getVar());
        }
        return features;
    }

    public static ClientInfo getClientInfo(final AccountJid account, final Jid jid) {
        DiscoverInfo discoverInfoByUser = EntityCapsManager.getDiscoverInfoByUser(jid);
        if (discoverInfoByUser == null) {
            Application.getInstance().runInBackground(new Runnable() {
                @Override
                public void run() {
                    try {
                        ServiceDiscoveryManager.getInstanceFor(AccountManager.getInstance().getAccount(account).getConnection()).discoverInfo(jid);
                    } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException | SmackException.NotConnectedException e) {
                        LogManager.exception(this, e);
                    }
                }
            });
            return null;
        } else {
            return getClientInfo(discoverInfoByUser);
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
