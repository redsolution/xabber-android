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

import android.support.annotation.NonNull;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnCloseListener;
import com.xabber.android.data.OnInitializedListener;
import com.xabber.android.data.OnTimerListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.listeners.OnAuthorizedListener;
import com.xabber.android.data.connection.listeners.OnConnectedListener;
import com.xabber.android.data.connection.listeners.OnConnectionListener;
import com.xabber.android.data.connection.listeners.OnDisconnectListener;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.connection.listeners.OnResponseListener;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.xmpp.address.Jid;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.sm.StreamManagementException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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

        ServiceDiscoveryManager.setDefaultIdentity(new DiscoverInfo.Identity("client", Application.getInstance()
                .getString(R.string.client_name), "handheld"));

        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(final XMPPConnection connection) {
                LogManager.i(this, "connectionCreated");
                ServiceDiscoveryManager.getInstanceFor(connection).addFeature("sslc2s");
            }
        });
    }

    /**
     * List of managed connection. Only managed connections can notify
     * registered listeners.
     */
    private final Collection<ConnectionThread> managedConnections;
    /**
     * Request holders for its packet id in accounts.
     */
    private final NestedMap<RequestHolder> requests;

    private ConnectionManager() {
        LogManager.i(this, "ConnectionManager");
        managedConnections = new ArrayList<>();
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
        ArrayList<ConnectionThread> connections = new ArrayList<>(managedConnections);
        managedConnections.clear();
        for (ConnectionThread connectionThread : connections) {
            connectionThread.getAccountItem().disconnect(connectionThread);
        }
    }

    /**
     * Update connection state.
     * <p/>
     * Start connections in waiting states and stop invalidated connections.
     *
     * @param userRequest
     */
    public void updateConnections(boolean userRequest) {
        LogManager.i(this, "updateConnections");

        AccountManager accountManager = AccountManager.getInstance();
        for (String account : accountManager.getAccounts()) {
            final ConnectionItem connectionItem = accountManager.getAccount(account);

            if (connectionItem.updateConnection(userRequest)) {
                AccountManager.getInstance().onAccountChanged(account);
            }
        }
    }

    /**
     * Disconnect and connect using new network.
     */
    public void forceReconnect() {
        LogManager.i(this, "forceReconnect");
        AccountManager accountManager = AccountManager.getInstance();
        for (String account : accountManager.getAccounts()) {
            accountManager.getAccount(account).forceReconnect();
            AccountManager.getInstance().onAccountChanged(account);
        }
    }

    /**
     * Send stanza to authenticated connection and and acknowledged listener if Stream Management is enabled on server.
     */
    public void sendStanza(String account, Message stanza, StanzaListener acknowledgedListener) throws NetworkException {
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
    public void sendStanza(String account, Stanza stanza) throws NetworkException {
        sendStanza(getXmppTcpConnection(account), stanza);
    }

    private void sendStanza(@NonNull XMPPTCPConnection xmppConnection, @NonNull Stanza stanza) throws NetworkException {
        try {
            xmppConnection.sendStanza(stanza);
        } catch (SmackException.NotConnectedException e) {
            NetworkException networkException = new NetworkException(R.string.XMPP_EXCEPTION);
            networkException.initCause(e);
            throw networkException;
        }
    }

    public @NonNull XMPPTCPConnection getXmppTcpConnection(String account) throws NetworkException {
        ConnectionThread connectionThread = null;
        for (ConnectionThread check : managedConnections) {
            if (check.getAccountItem() instanceof AccountItem
                    && ((AccountItem) check.getAccountItem()).getAccount().equals(account)) {
                connectionThread = check;
                break;
            }
        }
        if (connectionThread == null || !connectionThread.getAccountItem().getState().isConnected()) {
            throw new NetworkException(R.string.NOT_CONNECTED);
        }
        return (XMPPTCPConnection) connectionThread.getXMPPConnection();
    }

    /**
     * Send packet to authenticated connection. And notify listener about
     * acknowledgment.
     *
     * @param account
     * @param iq
     * @param listener
     * @throws NetworkException
     */
    public void sendRequest(String account, IQ iq, OnResponseListener listener) throws NetworkException {
        String stanzaId = iq.getStanzaId();
        RequestHolder holder = new RequestHolder(listener);
        sendStanza(account, iq);
        requests.put(account, stanzaId, holder);
    }

    public void onConnection(ConnectionThread connectionThread) {
        LogManager.i(this, "onConnection");
        managedConnections.add(connectionThread);
        for (OnConnectionListener listener : Application.getInstance().getManagers(OnConnectionListener.class)) {
            listener.onConnection(connectionThread.getAccountItem());
        }
    }

    public void onConnected(final ConnectionThread connectionThread) {
        LogManager.i(this, "onConnected");
        if (!managedConnections.contains(connectionThread)) {
            return;
        }

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (OnConnectedListener listener : Application.getInstance().getManagers(OnConnectedListener.class)) {
                    listener.onConnected(connectionThread.getAccountItem());
                }
            }
        });
    }

    public void onAuthorized(final ConnectionThread connectionThread) {
        if (!managedConnections.contains(connectionThread)) {
            return;
        }
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (OnAuthorizedListener listener : Application.getInstance().getManagers(OnAuthorizedListener.class)) {
                    listener.onAuthorized(connectionThread.getAccountItem());
                }

                AccountManager.getInstance().removeAuthorizationError(
                        ((AccountItem)connectionThread.getAccountItem()).getAccount());

            }
        });
    }

    public void onDisconnect(ConnectionThread connectionThread) {
        LogManager.i(this, "onDisconnect");
        if (!managedConnections.remove(connectionThread)) {
            return;
        }
        ConnectionItem connectionItem = connectionThread.getAccountItem();
        if (connectionItem instanceof AccountItem) {
            String account = ((AccountItem) connectionItem).getAccount();
            for (Entry<String, RequestHolder> entry : requests.getNested(account).entrySet()) {
                entry.getValue().getListener().onDisconnect(account, entry.getKey());
            }
            requests.clear(account);
        }
        for (OnDisconnectListener listener : Application.getInstance().getManagers(OnDisconnectListener.class)) {
            listener.onDisconnect(connectionThread.getAccountItem());
        }
    }

    public void processPacket(ConnectionThread connectionThread, Stanza stanza) {
        if (!managedConnections.contains(connectionThread)) {
            return;
        }
        ConnectionItem connectionItem = connectionThread.getAccountItem();
        if (stanza instanceof IQ && connectionItem instanceof AccountItem) {
            IQ iq = (IQ) stanza;
            String packetId = iq.getStanzaId();
            if (packetId != null && (iq.getType() == Type.result || iq.getType() == Type.error)) {
                String account = ((AccountItem) connectionItem).getAccount();
                RequestHolder requestHolder = requests.remove(account, packetId);
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
            listener.onPacket(connectionItem, Jid.getBareAddress(stanza.getFrom()), stanza);
        }
    }

    @Override
    public void onTimer() {
        long now = new Date().getTime();
        Iterator<NestedMap.Entry<RequestHolder>> iterator = requests.iterator();
        while (iterator.hasNext()) {
            NestedMap.Entry<RequestHolder> entry = iterator.next();
            if (entry.getValue().isExpired(now)) {
                entry.getValue().getListener().onTimeout(entry.getFirst(), entry.getSecond());
                iterator.remove();
            }
        }
    }

}
