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

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.roster.AccountRosterListener;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.parsing.ExceptionLoggingCallback;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.sm.predicates.ForEveryStanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;

import java.io.IOException;

/**
 * Provides connection workflow.
 *
 * @author alexander.ivanov
 */
public class ConnectionThread {

    private final AccountItem accountItem;
    private XMPPTCPConnection connection;

    public ConnectionThread(final AccountItem accountItem) {
        this.accountItem = accountItem;
    }

    public XMPPTCPConnection getXMPPConnection() {
        return connection;
    }

    public ConnectionItem getAccountItem() {
        return accountItem;
    }

    void start(final boolean registerNewAccount) {
        LogManager.i(this, "start. registerNewAccount " + registerNewAccount);

        ConnectionManager.getInstance().onConnection(this);

        Thread thread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        configureConnection();
                        connect();
                        if (registerNewAccount) {
                            createAccount();
                        }
                        login();
                    }
                },
                "Connection thread for " + accountItem.getRealJid());
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    public void configureConnection() {
        connection = ConnectionBuilder.build(accountItem.getConnectionSettings());

        // enable Stream Management support. SMACK will only enable SM if supported by the server,
        // so no additional checks are required.
        connection.setUseStreamManagement(true);

        // by default Smack disconnects in case of parsing errors
        connection.setParsingExceptionCallback(new ExceptionLoggingCallback());

        connection.addAsyncStanzaListener(everyStanzaListener, ForEveryStanza.INSTANCE);
        connection.addConnectionListener(connectionListener);

        AccountRosterListener rosterListener = new AccountRosterListener(accountItem.getAccount());
        final Roster roster = Roster.getInstanceFor(connection);
        roster.addRosterListener(rosterListener);
        roster.addRosterLoadedListener(rosterListener);
        roster.setSubscriptionMode(Roster.SubscriptionMode.manual);
    }


    private void connect() {
        try {
            connection.connect();
        } catch (SmackException e) {
            // There is no connection listeners yet, so we call onClose.
            LogManager.w(this, "Connection failed. SmackException " + e.getMessage());
        } catch (IOException e) {
            // There is no connection listeners yet, so we call onClose.
            LogManager.w(this, "Connection failed. IOException " + e.getMessage());
        } catch (XMPPException e) {
            // There is no connection listeners yet, so we call onClose.
            LogManager.w(this, "Connection failed. XMPPException " + e.getMessage());
        }
    }

    private void createAccount() {
        try {
            AccountManager.getInstance(connection)
                    .createAccount(accountItem.getConnectionSettings().getUserName(),
                            accountItem.getConnectionSettings().getPassword());
        } catch (SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
            LogManager.exception(accountItem, e);
            connectionListener.connectionClosedOnError(e);
            // Server will destroy connection, but we can speedup
            // it.
            connection.disconnect();
            return;
        }

        onAccountCreated();
    }

    private void onAccountCreated() {
        LogManager.i(this, "Account created");
        accountItem.onAccountRegistered(this);
    }

    private void login() {
        boolean success = false;

        try {
            connection.login();
            success = true;
        } catch (SASLErrorException saslErrorException) {
            LogManager.w(this, "Login failed. SASLErrorException."
                    + " SASLError: " + saslErrorException.getSASLFailure().getSASLError()
                    + " Mechanism: " + saslErrorException.getMechanism());
            LogManager.exception(this, saslErrorException);

            Application.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Login failed. We don`t want to reconnect.
                    accountItem.onAuthFailed();
                }
            });
            connectionListener.connectionClosed();
        } catch (XMPPException e) {
            LogManager.exception(this, e);
            connectionListener.connectionClosedOnError(e);
        } catch (SmackException | IOException e) {
            LogManager.exception(this, e);
        }

        if (!success) {
            connection.disconnect();
        }
    }

    private ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void connected(XMPPConnection connection) {
            LogManager.d(this, "connected " + getAccountItem().getRealJid());

            accountItem.onConnected(ConnectionThread.this);
            ConnectionManager.getInstance().onConnected(ConnectionThread.this);
        }

        @Override
        public void authenticated(XMPPConnection connection, boolean resumed) {
            LogManager.d(this, "authenticated " + getAccountItem().getRealJid() + " resumed " + resumed);

            PingManager.getInstanceFor(ConnectionThread.this.connection).registerPingFailedListener(pingFailedListener);

            accountItem.onAuthorized(ConnectionThread.this);
            ConnectionManager.getInstance().onAuthorized(ConnectionThread.this);
        }

        @Override
        public void connectionClosed() {
            LogManager.d(this, "connectionClosed " + getAccountItem().getRealJid());

            accountItem.onClose(ConnectionThread.this);
        }

        @Override
        public void connectionClosedOnError(final Exception e) {
            LogManager.d(this, "connectionClosedOnError " + getAccountItem().getRealJid() + " " + e.getMessage());

            PingManager.getInstanceFor(connection).unregisterPingFailedListener(pingFailedListener);
            connectionClosed();
        }

        @Override
        public void reconnectionSuccessful() {
            LogManager.d(this, "reconnectionSuccessful " + getAccountItem().getRealJid());
        }

        @Override
        public void reconnectingIn(int seconds) {
            LogManager.d(this, "reconnectingIn " + getAccountItem().getRealJid() + " " + seconds + " seconds");
        }

        @Override
        public void reconnectionFailed(Exception e) {
            LogManager.d(this, "reconnectionFailed " + getAccountItem().getRealJid() + " " + e.getMessage());
        }
    };

    private StanzaListener everyStanzaListener = new StanzaListener() {
        @Override
        public void processPacket(final Stanza stanza) throws SmackException.NotConnectedException {
            Application.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ConnectionManager.getInstance().processPacket(ConnectionThread.this, stanza);
                }
            });

        }
    };

    private PingFailedListener pingFailedListener = new PingFailedListener() {
        @Override
        public void pingFailed() {
            LogManager.i(this, "pingFailed for " + getAccountItem().getRealJid());
            getAccountItem().forceReconnect();
        }
    };

}
