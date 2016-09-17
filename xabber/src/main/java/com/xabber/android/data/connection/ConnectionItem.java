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
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.roster.AccountRosterListener;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.parsing.ExceptionLoggingCallback;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.sm.predicates.ForEveryStanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;

import java.io.IOException;

/**
 * Abstract connection.
 *
 * @author alexander.ivanov
 */
public abstract class ConnectionItem {

    private static final String LOG_TAG = ConnectionItem.class.getSimpleName();

    @NonNull
    private final AccountJid account;

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
    private XMPPTCPConnection connection;

    /**
     * Connection was requested by user.
     */
    private boolean isConnectionRequestedByUser;

    /**
     * Current state.
     */
    private ConnectionState state;

    /**
     * Whether force reconnection is in progress.
     */
    private boolean disconnectionRequested;

    /**
     * Need to register account on XMPP server.
     */
    private boolean registerNewAccount;

    @NonNull
    private final AccountRosterListener rosterListener;
    private Toast toast;

    private ConnectionThread connectionThread;

    public ConnectionItem(boolean custom,
                          String host, int port, DomainBareJid serverName, Localpart userName,
                          Resourcepart resource, boolean storePassword, String password,
                          boolean saslEnabled, TLSMode tlsMode, boolean compression,
                          ProxyType proxyType, String proxyHost, int proxyPort,
                          String proxyUser, String proxyPassword) {
        this.account = AccountJid.from(userName, serverName, resource);
        rosterListener = new AccountRosterListener(getAccount());
        connectionListener = new com.xabber.android.data.connection.ConnectionListener(this);

        connectionSettings = new ConnectionSettings(userName,
                serverName, resource, custom, host, port, password,
                saslEnabled, tlsMode, compression, proxyType, proxyHost,
                proxyPort, proxyUser, proxyPassword);
        createConnection();

        isConnectionRequestedByUser = false;
        disconnectionRequested = false;
        updateState(ConnectionState.offline);
    }

    private void createConnection() {
        showDebugToast("createConnection...");

        connection = ConnectionBuilder.build(connectionSettings);
        connectionThread = new ConnectionThread(connection, this);

        addConnectionListeners();
        configureConnection();
    }


    @NonNull
    public AccountJid getAccount() {
        return account;
    }

    /**
     * Register new account on server.
     */
    public void registerAccount() {
        registerNewAccount = true;
    }

    /**
     * Report if this connection is to register a new account on XMPP server.
     */
    public boolean isRegisterAccount() {
        return registerNewAccount;
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

    public ConnectionState getState() {
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

    /**
     * @param userRequest action was requested by user.
     * @return Whether connection is available.
     */
    protected abstract boolean isConnectionAvailable(boolean userRequest);

    public void connect() {
        updateState(ConnectionState.connecting);
        if (connectionThread == null) {
            connectionThread = new ConnectionThread(connection, this);
        };

        connectionThread.start();
    }

    private void configureConnection() {
        // enable Stream Management support. SMACK will only enable SM if supported by the server,
        // so no additional checks are required.
        connection.setUseStreamManagement(true);

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

        PingManager.getInstanceFor(connection).registerPingFailedListener(pingFailedListener);
    }


    /**
     * Starts disconnection in another thread.
     */
    protected static void disconnect(final AbstractXMPPConnection xmppConnection) {
        Thread thread = new Thread("Disconnection thread for " + xmppConnection) {
            @Override
            public void run() {
                xmppConnection.disconnect();
            }

        };
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    public abstract void onAuthFailed();

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
                connection.disconnect();
            }

        };
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    void recreateConnection() {
        showDebugToast("recreateConnection...");

        PingManager.getInstanceFor(connection).unregisterPingFailedListener(pingFailedListener);

        connection.removeConnectionListener(connectionListener);
        connection.removeAsyncStanzaListener(everyStanzaListener);
        final Roster roster = Roster.getInstanceFor(connection);
        roster.removeRosterLoadedListener(rosterListener);
        roster.removeRosterListener(rosterListener);

        createConnection();
    }

    void updateState(ConnectionState newState) {
        ConnectionState prevState = this.state;

        if (connection.isAuthenticated()) {
            this.state = ConnectionState.connected;
        } else if (connection.isConnected()) {
            if (newState == ConnectionState.authentication || newState == ConnectionState.registration) {
                this.state = newState;
            }
        } else {
            switch (newState) {
                case offline:
                case waiting:
                case connecting:
                    this.state = newState;
                    break;
            }
        }

        if (prevState != state) {
            AccountManager.getInstance().onAccountChanged(getAccount());
        }
    }


    private StanzaListener everyStanzaListener = new StanzaListener() {
        @Override
        public void processPacket(final Stanza stanza) throws SmackException.NotConnectedException {
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
            showDebugToast("ping failed");
            LogManager.i(this, "pingFailed for " + getAccount());
            updateState(ConnectionState.offline);
            disconnect();
        }
    };

    void showDebugToast(final String message) {
        showDebugToast(message, Toast.LENGTH_LONG);
    }

    void showDebugToast(final String message, final int duration) {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (toast != null) {
                    toast.cancel();
                }
                toast = Toast.makeText(Application.getInstance(), message, Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }


}
