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

import android.widget.Toast;

import androidx.annotation.NonNull;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.xtoken.XToken;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.AccountRosterListener;
import com.xabber.xmpp.smack.XMPPTCPConnection;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.parsing.ExceptionLoggingCallback;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.sm.predicates.ForEveryStanza;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;

/**
 * Abstract connection.
 *
 * @author alexander.ivanov
 */
public abstract class ConnectionItem {

    @NonNull
    private final AccountJid account;

    final String logTag;

    /**
     * Connection options.
     */
    @NonNull
    private final ConnectionSettings connectionSettings;

    @NonNull
    private final com.xabber.android.data.connection.ConnectionListener connectionListener;

    /**
     * XMPP connection.
     */
    @NonNull
    XMPPTCPConnection connection;
    public static long defaultReplyTimeout = 30000; // in ms

    /**
     * Current state.
     */
    private ConnectionState state;
    /**
     * Whether the connection is outdated or not and needs to be recreated
     */
    private boolean connectionIsOutdated;

    @NonNull
    private final AccountRosterListener rosterListener;
    Toast toast;

    private ConnectionThread connectionThread;

    public ConnectionItem(boolean custom,
                          String host, int port, DomainBareJid serverName, Localpart userName,
                          Resourcepart resource, boolean storePassword, String password, String token,
                          XToken xToken, boolean saslEnabled, TLSMode tlsMode, boolean compression,
                          ProxyType proxyType, String proxyHost, int proxyPort,
                          String proxyUser, String proxyPassword) {
        this.account = AccountJid.from(userName, serverName, resource);
        this.logTag = getClass().getSimpleName() + ": " + account;
        rosterListener = new AccountRosterListener(getAccount());
        connectionListener = new com.xabber.android.data.connection.ConnectionListener(this);

        connectionSettings = new ConnectionSettings(userName,
                serverName, resource, custom, host, port, password, token, xToken,
                saslEnabled, tlsMode, compression, proxyType, proxyHost,
                proxyPort, proxyUser, proxyPassword);
        connection = createConnection();

        updateState(ConnectionState.offline);
    }

    private XMPPTCPConnection createConnection() {
        connection = ConnectionBuilder.build(account, connectionSettings);
        LogManager.i(logTag, "Connection created");

        connectionThread = new ConnectionThread(connection, this);

        addConnectionListeners();
        configureConnection();

        return connection;
    }


    @NonNull
    public AccountJid getAccount() {
        return account;
    }

    @NonNull
    public XMPPTCPConnection getConnection() {
        return connection;
    }

    /**
     * @return connection options.
     */
    @NonNull
    public ConnectionSettings getConnectionSettings() {
        return connectionSettings;
    }

    public synchronized ConnectionState getState() {
        return state;
    }

    /**
     * Returns real full jid, that was assigned while login.
     *
     * @return <code>null</code> if connection is not established.
     */
    public EntityFullJid getRealJid() {
        return connection.getUser();
    }

    public boolean connect() {
        LogManager.i(logTag, "connect");

        if(getState() == ConnectionState.disconnecting || getState() == ConnectionState.waiting) {
            // if we wanted to connect during the disconnection process, we
            // need to make sure our connection settings aren't outdated.
            checkIfConnectionIsOutdated();
        }
        if (connectionThread == null) {
            connectionThread = new ConnectionThread(connection, this);
        }

        return connectionThread.start();
    }

    private void configureConnection() {
        // enable Stream Management support. SMACK will only enable SM if supported by the server,
        // so no additional checks are required.
        connection.setUseStreamManagement(true);
        connection.setUseStreamManagementResumption(false);

        // by default Smack disconnects in case of parsing errors
        connection.setParsingExceptionCallback(new ExceptionLoggingCallback());

    }

    private void addConnectionListeners() {
        final Roster roster = Roster.getInstanceFor(connection);
        roster.addRosterListener(rosterListener);
        roster.addRosterLoadedListener(rosterListener);
        roster.setSubscriptionMode(Roster.SubscriptionMode.manual);
        roster.setRosterLoadedAtLogin(true);

        connection.addAsyncStanzaListener(everyStanzaListener, ForEveryStanza.INSTANCE);
        connection.addConnectionListener(connectionListener);

        refreshPingFailedListener(true);
    }

    /**
     * Update password.
     */
    protected void onPasswordChanged(String password) {
        connectionSettings.setPassword(password);
    }

    public void disconnect() {
        Thread thread = new Thread("Disconnection thread for " + connection) {
            @Override
            public void run() {
                LogManager.i(logTag, "disconnect");

                if (connection.isConnected()) {
                    updateState(ConnectionState.disconnecting);
                    LogManager.i(logTag, "connected now, disconnecting...");
                    connection.disconnect();
                } else {
                    LogManager.i(logTag, "already disconnected");
                }
            }

        };
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    public void recreateConnection() {
        LogManager.i(logTag, "recreateConnection");

        Thread thread = new Thread("Disconnection thread for " + connection) {
            @Override
            public void run() {
                updateState(ConnectionState.disconnecting);
                connection.disconnect();

                Application.getInstance().runOnUiThread(() -> createNewConnection());

            }

        };
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    public void recreateConnectionWithEnable(final AccountJid account) {
        LogManager.i(logTag, "recreateConnection");

        Thread thread = new Thread("Disconnection thread for " + connection) {
            @Override
            public void run() {
                updateState(ConnectionState.disconnecting);
                connection.disconnect();

                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        createNewConnection();
                        AccountManager.getInstance().setEnabled(account, true);
                    }
                });

            }

        };
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    @SuppressWarnings("WeakerAccess")
    void createNewConnection() {
        LogManager.i(logTag, "createNewConnection");

        refreshPingFailedListener(false);

        connection.removeConnectionListener(connectionListener);
        connection.removeAsyncStanzaListener(everyStanzaListener);
        final Roster roster = Roster.getInstanceFor(connection);
        roster.removeRosterLoadedListener(rosterListener);
        roster.removeRosterListener(rosterListener);

        createConnection();
        ReconnectionManager.getInstance().resetReconnectionInfo(account);
    }

    void updateState(ConnectionState newState) {
        boolean changed = setState(newState);

        if (changed) {
            EventBus.getDefault().post(new ConnectionStateChangedEvent(newState));
            if (newState == ConnectionState.connected) {
                AccountManager.getInstance().setSuccessfulConnectionHappened(account, true);
            }

            AccountManager.getInstance().onAccountChanged(getAccount());
        }
    }

    private synchronized boolean setState(ConnectionState newState) {
        ConnectionState prevState = this.state;

        this.state = newState;

        LogManager.i(logTag, "updateState. prev " + prevState + " new "  + newState);

        return prevState != state;
    }

    public void setConnectionIsOutdated(boolean outdated) {
        connectionIsOutdated = outdated;
    }

    // recreates the connection if it's outdated
    public void checkIfConnectionIsOutdated() {
        if (connectionIsOutdated) {
            LogManager.d(logTag, "connection is outdated, creating a new one");
            connectionIsOutdated = false;
            createNewConnection();
        } else {
            LogManager.d(logTag, "connection is not outdated");
        }
    }

    public void refreshPingFailedListener(boolean register) {
        PingManager.getInstanceFor(connection).unregisterPingFailedListener(pingFailedListener);
        if (register) PingManager.getInstanceFor(connection).registerPingFailedListener(pingFailedListener);
    }

    private StanzaListener everyStanzaListener = new StanzaListener() {
        @Override
        public void processStanza(final Stanza stanza) throws SmackException.NotConnectedException {
            Application.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (OnPacketListener listener : Application.getInstance().getManagers(OnPacketListener.class)) {
                        listener.onStanza(ConnectionItem.this, stanza);
                    }
                }
            });
        }
    };

    private PingFailedListener pingFailedListener = new PingFailedListener() {
        @Override
        public void pingFailed() {
            LogManager.i(this, "pingFailed for " + getAccount());
            updateState(ConnectionState.offline);
            disconnect();
        }
    };

    public static class ConnectionStateChangedEvent {
        ConnectionState connectionState;

        ConnectionStateChangedEvent(ConnectionState connectionState){
            this.connectionState = connectionState;
        }

        public ConnectionState getConnectionState() { return connectionState; }
    }

}
