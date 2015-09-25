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

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.account.AccountProtocol;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.XMPPConnection;

/**
 * Abstract connection.
 *
 * @author alexander.ivanov
 */
public abstract class ConnectionItem {

    /**
     * Connection options.
     */
    private final ConnectionSettings connectionSettings;

    /**
     * XMPP connection.
     */
    private ConnectionThread connectionThread;

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

    public ConnectionItem(AccountProtocol protocol, boolean custom,
                          String host, int port, String serverName, String userName,
                          String resource, boolean storePassword, String password,
                          boolean saslEnabled, TLSMode tlsMode, boolean compression,
                          ProxyType proxyType, String proxyHost, int proxyPort,
                          String proxyUser, String proxyPassword) {
        connectionSettings = new ConnectionSettings(protocol, userName,
                serverName, resource, custom, host, port, password,
                saslEnabled, tlsMode, compression, proxyType, proxyHost,
                proxyPort, proxyUser, proxyPassword);
        isConnectionRequestedByUser = false;
        disconnectionRequested = false;
        connectionThread = null;
        state = ConnectionState.offline;
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
        return(registerNewAccount);
    }

    /**
     * Gets current connection thread.
     *
     * @return <code>null</code> if thread doesn't exists.
     */
    public ConnectionThread getConnectionThread() {
        return connectionThread;
    }

    /**
     * @return connection options.
     */
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
    public String getRealJid() {
        ConnectionThread connectionThread = getConnectionThread();
        if (connectionThread == null) {
            return null;
        }
        XMPPConnection xmppConnection = connectionThread.getXMPPConnection();
        if (xmppConnection == null) {
            return null;
        }
        String user = xmppConnection.getUser();
        if (user == null) {
            return null;
        }
        return user;
    }

    /**
     * @param userRequest action was requested by user.
     * @return Whether connection is available.
     */
    protected boolean isConnectionAvailable(boolean userRequest) {
        return true;
    }

    /**
     * Connect or disconnect from server depending on internal flags.
     *
     * @param userRequest action was requested by user.
     * @return Whether state has been changed.
     */
    public boolean updateConnection(boolean userRequest) {
        boolean available = isConnectionAvailable(userRequest);
        if (NetworkManager.getInstance().getState() != NetworkState.available
                || !available || disconnectionRequested) {
            ConnectionState target = available ? ConnectionState.waiting : ConnectionState.offline;
            if (state == ConnectionState.connected || state == ConnectionState.authentication
                    || state == ConnectionState.connecting) {
                if (userRequest) {
                    isConnectionRequestedByUser = false;
                }
                if (connectionThread != null) {
                    disconnect(connectionThread);
                    // Force remove managed connection thread.
                    onClose(connectionThread);
                    connectionThread = null;
                }
            } else if (state == target) {
                return false;
            }
            state = target;
            return true;
        } else {
            if (state == ConnectionState.offline || state == ConnectionState.waiting) {
                if (userRequest) {
                    isConnectionRequestedByUser = true;
                }
                state = ConnectionState.connecting;
                connectionThread = new ConnectionThread(this);

                boolean useSRVLookup;
                String fullyQualifiedDomainName;
                int port;
                if (connectionSettings.isCustomHostAndPort()) {
                    fullyQualifiedDomainName = connectionSettings.getHost();
                    port = connectionSettings.getPort();
                    useSRVLookup = false;
                } else {
                    fullyQualifiedDomainName = connectionSettings.getServerName();
                    port = 5222;
                    useSRVLookup = true;
                }

                connectionThread.start(fullyQualifiedDomainName, port, useSRVLookup, registerNewAccount);

                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Disconnect and connect using new connection.
     */
    public void forceReconnect() {
        if (!getState().isConnectable()) {
            return;
        }
        disconnectionRequested = true;
        boolean request = isConnectionRequestedByUser;
        isConnectionRequestedByUser = false;
        updateConnection(false);
        isConnectionRequestedByUser = request;
        disconnectionRequested = false;
        updateConnection(false);
    }

    /**
     * Starts disconnection in another thread.
     */
    protected void disconnect(final ConnectionThread connectionThread) {
        Thread thread = new Thread("Disconnection thread for " + this) {
            @Override
            public void run() {
                AbstractXMPPConnection xmppConnection = connectionThread.getXMPPConnection();
                if (xmppConnection != null)
                    try {
                        xmppConnection.disconnect();
                    } catch (RuntimeException e) {
                        // connectionClose() in smack can fail.
                    }
            }

        };
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * @param connectionThread
     * @return Whether thread is managed by connection.
     */
    boolean isManaged(ConnectionThread connectionThread) {
        return connectionThread == this.connectionThread;
    }

    /**
     * Update password.
     *
     * @param password
     */
    protected void onPasswordChanged(String password) {
        connectionSettings.setPassword(password);
    }

    /**
     * SRV record has been resolved.
     */
    protected void onSRVResolved(ConnectionThread connectionThread) {
    }

    /**
     * Invalid certificate has been received.
     */
    protected void onInvalidCertificate() {
    }

    /**
     * Connection has been established.
     */
    protected void onConnected(ConnectionThread connectionThread) {
        if (isRegisterAccount()) {
            state = ConnectionState.registration;
        } else if (isManaged(connectionThread)) {
            state = ConnectionState.authentication;
        }
    }

    /**
     * New account has been registered on XMPP server.
     */
    protected void onAccountRegistered(ConnectionThread connectionThread) {
        registerNewAccount = false;
        if (isManaged(connectionThread)) {
            state = ConnectionState.authentication;
        }
    }

    /**
     * Authorization failed.
     */
    protected void onAuthFailed() {
    }

    /**
     * Authorization passed.
     */
    protected void onAuthorized(ConnectionThread connectionThread) {
        if (isManaged(connectionThread)) {
            state = ConnectionState.connected;
        }
    }

    /**
     * Called when disconnect should occur.
     *
     * @param connectionThread
     * @return <code>true</code> if connection thread was managed.
     */
    private boolean onDisconnect(ConnectionThread connectionThread) {
        XMPPConnection xmppConnection = connectionThread.getXMPPConnection();
        boolean acceptable = isManaged(connectionThread);
        if (xmppConnection == null) {
            LogManager.i(this, "onClose " + acceptable);
        } else {
            LogManager.i(this, "onClose " + xmppConnection.hashCode() + " - "
                            + xmppConnection.getConnectionCounter() + ", " + acceptable);
        }

        ConnectionManager.getInstance().onDisconnect(connectionThread);
        if (acceptable) {
            connectionThread.shutdown();
        }
        return acceptable;
    }

    /**
     * Called when connection was closed for some reason.
     */
    protected void onClose(ConnectionThread connectionThread) {
        if (onDisconnect(connectionThread)) {
            state = ConnectionState.waiting;
            this.connectionThread = null;
            if (isConnectionRequestedByUser) {
                Application.getInstance().onError(R.string.CONNECTION_FAILED);
            }
            isConnectionRequestedByUser = false;
        }
    }

    /**
     * Called when another host should be used.
     *
     * @param connectionThread
     * @param fqdn
     * @param port
     * @param useSrvLookup
     */
    protected void onSeeOtherHost(ConnectionThread connectionThread,
                                  String fqdn, int port, boolean useSrvLookup) {
        // TODO: Check for number of redirects.
        if (onDisconnect(connectionThread)) {
            state = ConnectionState.connecting;
            this.connectionThread = new ConnectionThread(this);
            this.connectionThread.start(fqdn, port, useSrvLookup, registerNewAccount);
        }
    }

}
