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

import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnCloseListener;
import com.xabber.android.data.OnInitializedListener;
import com.xabber.android.data.OnTimerListener;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.listeners.OnAuthorizedListener;
import com.xabber.android.data.connection.listeners.OnConnectedListener;
import com.xabber.android.data.connection.listeners.OnDisconnectListener;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.connection.listeners.OnResponseListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.roster.RosterManager;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.sm.StreamManagementException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.caps.EntityCapsManager;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Connection manager.
 *
 * @author alexander.ivanov
 */
public class ConnectionManager implements OnInitializedListener, OnCloseListener, OnTimerListener {

    /**
     * Timeout for receiving reply from server.
     */
    public final static int PACKET_REPLY_TIMEOUT = 30000;

    public final static int PING_INTERVAL_SECONDS = 30;

    private final static ConnectionManager instance;

    static {
        instance = new ConnectionManager();
        Application.getInstance().addManager(instance);

        SmackConfiguration.setDefaultPacketReplyTimeout(PACKET_REPLY_TIMEOUT);

        String applicationFullTitle = Application.getInstance().getString(R.string.application_title_full);

        String versionName = null;
        try {
            versionName = Application.getInstance().getPackageManager().getPackageInfo(
                    Application.getInstance().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String clientIdentity;

        if (!TextUtils.isEmpty(versionName)) {
            clientIdentity = applicationFullTitle + " " + versionName;
        } else {
            clientIdentity = applicationFullTitle;
        }

        ServiceDiscoveryManager.setDefaultIdentity(new DiscoverInfo.Identity("client", clientIdentity, "handheld"));
        EntityCapsManager.setDefaultEntityNode(Application.getInstance()
                .getString(R.string.caps_entity_node));
    }

    /**
     * List of managed connection. Only managed connections can notify
     * registered listeners.
     */
    private final Collection<AbstractXMPPConnection> managedConnections;
    /**
     * Request holders for its packet id in accounts.
     */
    private final NestedMap<RequestHolder> requests;

    private ConnectionManager() {
        LogManager.i(this, "ConnectionManager");
        managedConnections = new HashSet<>();
        requests = new NestedMap<>();
        org.jivesoftware.smackx.ping.PingManager.setDefaultPingInterval(PING_INTERVAL_SECONDS);
    }

    public static ConnectionManager getInstance() {
        return instance;
    }

    @Override
    public void onInitialized() {
        LogManager.i(this, "onInitialized");
        updateConnections(false);
        AccountManager.getInstance().onAccountsChanged(new ArrayList<>(AccountManager.getInstance().getAllAccounts()));
    }

    @Override
    public void onClose() {
        LogManager.i(this, "onClose");
        ArrayList<AbstractXMPPConnection> connections = new ArrayList<>(managedConnections);
        managedConnections.clear();
        for (AbstractXMPPConnection connection : connections) {
            ConnectionItem.disconnect(connection);
        }
    }

    /**
     * Update connection state.
     * <p/>
     * Start connections in waiting states and stop invalidated connections.
     */
    public void updateConnections(boolean userRequest) {
        LogManager.i(this, "updateConnections");

        AccountManager accountManager = AccountManager.getInstance();
        for (AccountJid account : accountManager.getAccounts()) {
            accountManager.getAccount(account).updateConnection(userRequest);
        }
    }

    /**
     * Disconnect and connect using new network.
     */
    public void forceReconnect() {
        LogManager.i(this, "forceReconnect");
        AccountManager accountManager = AccountManager.getInstance();
        for (AccountJid account : accountManager.getAccounts()) {
            accountManager.getAccount(account).forceReconnect();
        }
    }

    public void reconnect() {
        LogManager.i(this, "reconnect");
        AccountManager accountManager = AccountManager.getInstance();
        for (AccountJid account : accountManager.getAccounts()) {
            accountManager.getAccount(account).reconnect();
        }
    }

    /**
     * Send stanza to authenticated connection and and acknowledged listener if Stream Management is enabled on server.
     */
    public void sendStanza(AccountJid account, Message stanza, StanzaListener acknowledgedListener) throws NetworkException {
        XMPPTCPConnection xmppConnection = getXmppTcpConnection(account);

        if (xmppConnection.isSmEnabled()) {
            try {
                xmppConnection.addStanzaIdAcknowledgedListener(stanza.getStanzaId(), acknowledgedListener);
            } catch (StreamManagementException.StreamManagementNotEnabledException e) {
                LogManager.exception(this, e);
            }
        }

        sendStanza(xmppConnection, stanza);
    }

    /**
     * Send stanza to authenticated connection.
     */
    public void sendStanza(AccountJid account, Stanza stanza) throws NetworkException {
        sendStanza(getXmppTcpConnection(account), stanza);
    }

    private void sendStanza(@NonNull XMPPTCPConnection xmppConnection, @NonNull Stanza stanza) throws NetworkException {
        try {
            xmppConnection.sendStanza(stanza);
        } catch (SmackException.NotConnectedException e) {
            NetworkException networkException = new NetworkException(R.string.XMPP_EXCEPTION);
            networkException.initCause(e);
            throw networkException;
        } catch (InterruptedException e) {
            LogManager.exception(this, e);
        }
    }

    private @NonNull XMPPTCPConnection getXmppTcpConnection(AccountJid account) throws NetworkException {
        XMPPTCPConnection returnConnection = AccountManager.getInstance().getAccount(account).getConnection();
        if (!returnConnection.isAuthenticated()) {
            throw new NetworkException(R.string.NOT_CONNECTED);
        }
        return returnConnection;
    }

    /**
     * Send packet to authenticated connection. And notify listener about
     * acknowledgment.
     * @throws NetworkException
     */
    public void sendRequest(AccountJid account, IQ iq, OnResponseListener listener) throws NetworkException {
        String stanzaId = iq.getStanzaId();
        RequestHolder holder = new RequestHolder(listener);
        sendStanza(account, iq);
        requests.put(account.toString(), stanzaId, holder);
    }

    public void onConnection(AbstractXMPPConnection connection) {
        LogManager.i(this, "onConnection " + connection.getUser());
        managedConnections.add(connection);
    }

    public void onConnected(final ConnectionItem connectionItem) {
        if (!managedConnections.contains(connectionItem.getConnection())) {
            LogManager.i(this, "onConnected !managedConnections.contains(connectionThread)");
            onConnection(connectionItem.getConnection());
        }

        AccountManager.getInstance().onAccountChanged(connectionItem.getAccount());

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (OnConnectedListener listener : Application.getInstance().getManagers(OnConnectedListener.class)) {
                    listener.onConnected(connectionItem);
                }
            }
        });
    }

    public void onAuthorized(final ConnectionItem connectionItem, boolean resumed) {
        if (!managedConnections.contains(connectionItem.getConnection())) {
            LogManager.i(this, "onAuthorized !managedConnections.contains(connectionItem)");
            onConnection(connectionItem.getConnection());
            return;
        }

        AccountManager.getInstance().onAccountChanged(connectionItem.getAccount());

        if (resumed) {
            RosterManager.getInstance().updateContacts();
        }


        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (OnAuthorizedListener listener : Application.getInstance().getManagers(OnAuthorizedListener.class)) {
                    listener.onAuthorized(connectionItem);
                }
                AccountManager.getInstance().removeAuthorizationError(connectionItem.getAccount());

            }
        });
    }

    public void onDisconnect(ConnectionItem connectionItem) {
        XMPPTCPConnection connection = connectionItem.getConnection();

        LogManager.i(this, "onDisconnect " + connection.getUser());
        if (!managedConnections.remove(connection)) {
            return;
        }
        AccountJid account = connectionItem.getAccount();
        for (Entry<String, RequestHolder> entry : requests.getNested(account.toString()).entrySet()) {
            entry.getValue().getListener().onDisconnect(account, entry.getKey());
        }
        requests.clear(account.toString());
        for (OnDisconnectListener listener : Application.getInstance().getManagers(OnDisconnectListener.class)) {
            listener.onDisconnect(connectionItem);
        }
    }

    public void processPacket(ConnectionItem connectionItem, Stanza stanza) {
        if (!managedConnections.contains(connectionItem.getConnection())) {
            return;
        }
        if (stanza instanceof IQ) {
            IQ iq = (IQ) stanza;
            String packetId = iq.getStanzaId();
            if (packetId != null && (iq.getType() == Type.result || iq.getType() == Type.error)) {
                AccountJid account = connectionItem.getAccount();
                RequestHolder requestHolder = requests.remove(account.toString(), packetId);
                if (requestHolder != null) {
                    if (iq.getType() == Type.result) {
                        requestHolder.getListener().onReceived(account, packetId, iq);
                    } else {
                        requestHolder.getListener().onError(account, packetId, iq);
                    }
                }
            }
        }
        for (OnPacketListener listener : Application.getInstance().getManagers(OnPacketListener.class)) {
            listener.onStanza(connectionItem, stanza);
        }
    }

    @Override
    public void onTimer() {
        long now = new Date().getTime();
        Iterator<NestedMap.Entry<RequestHolder>> iterator = requests.iterator();
        while (iterator.hasNext()) {
            NestedMap.Entry<RequestHolder> entry = iterator.next();
            if (entry.getValue().isExpired(now)) {

                AccountJid account = null;
                try {
                    account = AccountJid.from(entry.getFirst());
                } catch (XmppStringprepException e) {
                    LogManager.exception(this, e);
                }

                entry.getValue().getListener().onTimeout(account, entry.getSecond());
                iterator.remove();
            }
        }
    }

}
