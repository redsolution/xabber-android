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

import android.support.annotation.Nullable;

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;

/**
 * Represent information about client.
 *
 * @author alexander.ivanov
 */
public class ClientInfo {

    public static final ClientInfo INVALID_CLIENT_INFO = new ClientInfo(null, null, null);

    private final String type;
    private final String name;
    private final ClientSoftware clientSoftware;

    static ClientInfo fromDiscoveryInfo(@Nullable DiscoverInfo discoverInfo) {
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
                    return new ClientInfo(identity.getType(), identity.getName(), discoverInfo.getNode());
                }
            }
        }
        return new ClientInfo(null, null, null);
    }

    private ClientInfo(String type, String name, String node) {
        this.type = type;
        this.name = name;
        this.clientSoftware = ClientSoftware.getByName(name, node);
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public ClientSoftware getClientSoftware() {
        return clientSoftware;
    }
}
