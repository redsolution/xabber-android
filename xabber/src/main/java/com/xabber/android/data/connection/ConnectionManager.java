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
package com.xabber.android.data.connection;

import com.xabber.android.data.OnCloseListener;
import com.xabber.android.data.OnInitializedListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;

import org.jivesoftware.smack.SmackConfiguration;

import java.util.ArrayList;

/**
 * Connection manager.
 *
 * @author alexander.ivanov
 */
public class ConnectionManager implements OnInitializedListener, OnCloseListener {

    private static String LOG_TAG = ConnectionManager.class.getSimpleName();

    /**
     * Timeout for receiving reply from server.
     */
    public final static int PACKET_REPLY_TIMEOUT = 30000;

    private final static int PING_INTERVAL_SECONDS = 60;

    private static ConnectionManager instance;

    public static ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }

        return instance;
    }

    private ConnectionManager() {
        LogManager.i(LOG_TAG, "ConnectionManager");
        org.jivesoftware.smackx.ping.PingManager.setDefaultPingInterval(PING_INTERVAL_SECONDS);
        SmackConfiguration.setDefaultReplyTimeout(PACKET_REPLY_TIMEOUT);
        /*
            Fix working with Nimbuzz.com
            Smack have error - ServiceDiscoveryManager: Exception while discovering info for
                                feature urn:xmpp:http:upload:0 of  conference....com node: null

            That exception shows that HttpFileUploadManager was unable to discover an service
            So disabling HttpFileUploadManager by default fixed this error

            HttpFileUploadManager will enabled later in ConnectionListener.authenticated() if server support it
         */
        SmackConfiguration.addDisabledSmackClass("org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager");
    }

    @Override
    public void onInitialized() {
        LogManager.i(LOG_TAG, "onInitialized");
        AccountManager.getInstance().onAccountsChanged(new ArrayList<>(AccountManager.getInstance().getAllAccounts()));
    }

    @Override
    public void onClose() {
        LogManager.i(LOG_TAG, "onClose");
        for (AccountJid accountJid : AccountManager.getInstance().getEnabledAccounts()) {
            AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
            if (accountItem != null) {
                accountItem.disconnect();
            }
        }
    }

    public void connectAll() {
        LogManager.i(LOG_TAG, "connectAll");
        AccountManager accountManager = AccountManager.getInstance();
        for (AccountJid account : accountManager.getEnabledAccounts()) {
            ReconnectionManager.getInstance().requestReconnect(account);
        }
    }
}

