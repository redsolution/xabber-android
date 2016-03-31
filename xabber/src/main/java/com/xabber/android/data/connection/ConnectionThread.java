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

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.roster.AccountRosterListener;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.parsing.ExceptionLoggingCallback;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.sasl.provided.SASLPlainMechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.ping.PingFailedListener;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;

/**
 * Provides connection workflow.
 *
 * @author alexander.ivanov
 */
public class ConnectionThread implements
        ConnectionListener,
        StanzaListener, PingFailedListener {

    /**
     * Filter to process all packets.
     */
    private final AcceptAll ACCEPT_ALL = new AcceptAll();

    private final ConnectionItem connectionItem;

    /**
     * SMACK connection.
     */
    private XMPPTCPConnection xmppConnection;

    /**
     * Thread holder for this connection.
     */
    private final ExecutorService executorService;

    private boolean started;

    private boolean registerNewAccount;
    private ConnectionSettings connectionSettings;

    public ConnectionThread(final ConnectionItem connectionItem) {
        LogManager.i(this, "NEW connection thread " + connectionItem.getRealJid());

        this.connectionItem = connectionItem;
        executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread thread = new Thread(
                                runnable,
                                "Connection thread for "
                                        + (connectionItem instanceof AccountItem ? ((AccountItem) connectionItem)
                                        .getAccount() : connectionItem));
                        thread.setPriority(Thread.MIN_PRIORITY);
                        thread.setDaemon(true);
                        return thread;
                    }
                });
        ConnectionManager.getInstance().onConnection(this);
        started = false;
    }

    public AbstractXMPPConnection getXMPPConnection() {
        return xmppConnection;
    }

    public ConnectionItem getConnectionItem() {
        return connectionItem;
    }

    /**
     * Start connection.
     * <p/>
     * This function can be called only once.
     */
    synchronized void start(final boolean registerNewAccount) {
        LogManager.i(this, "start. registerNewAccount " + registerNewAccount);

        connectionSettings = connectionItem.getConnectionSettings();

        if (started) {
            throw new IllegalStateException();
        }
        started = true;
        this.registerNewAccount = registerNewAccount;

        runOnConnectionThread(new Runnable() {
            @Override
            public void run() {
                createConnection();
                connect();
            }
        });
    }

    /**
     * Stop connection.
     * <p/>
     * start MUST BE CALLED FIRST.
     */
    void shutdown() {
        executorService.shutdownNow();
    }

    private void createConnection() {
        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();

        builder.setServiceName(connectionSettings.getServerName());

        if (connectionSettings.isCustomHostAndPort()) {
            builder.setHost(connectionSettings.getHost());
            builder.setPort(connectionSettings.getPort());
        }

        builder.setSecurityMode(connectionSettings.getTlsMode().getSecurityMode());
        builder.setCompressionEnabled(connectionSettings.useCompression());
        builder.setSendPresence(false);

        try {
            if (SettingsManager.securityCheckCertificate()) {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                MemorizingTrustManager mtm = new MemorizingTrustManager(Application.getInstance());
                sslContext.init(null, new X509TrustManager[]{mtm}, new java.security.SecureRandom());
                builder.setCustomSSLContext(sslContext);
                builder.setHostnameVerifier(
                        mtm.wrapHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier()));
            } else {
                TLSUtils.acceptAllCertificates(builder);
                TLSUtils.disableHostnameVerificationForTlsCertificicates(builder);
            }
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LogManager.exception(this, e);
        }

        setUpSASL();

        xmppConnection = new XMPPTCPConnection(builder.build());
        xmppConnection.addAsyncStanzaListener(this, ACCEPT_ALL);
        xmppConnection.addConnectionListener(this);

        // enable Stream Management support. SMACK will only enable SM if supported by the server,
        // so no additional checks are required.
        xmppConnection.setUseStreamManagement(true);

        // by default Smack disconnects in case of parsing errors
        xmppConnection.setParsingExceptionCallback(new ExceptionLoggingCallback());

        AccountRosterListener rosterListener = new AccountRosterListener(((AccountItem)connectionItem).getAccount());
        final Roster roster = Roster.getInstanceFor(xmppConnection);
        roster.addRosterListener(rosterListener);
        roster.addRosterLoadedListener(rosterListener);
        roster.setSubscriptionMode(Roster.SubscriptionMode.manual);

        org.jivesoftware.smackx.ping.PingManager.getInstanceFor(xmppConnection).registerPingFailedListener(this);
    }

    private void setUpSASL() {
        if (SettingsManager.connectionUsePlainTextAuth()) {
            final Map<String, String> registeredSASLMechanisms = SASLAuthentication.getRegisterdSASLMechanisms();
            for (String mechanism : registeredSASLMechanisms.values()) {
                SASLAuthentication.blacklistSASLMechanism(mechanism);
            }

            SASLAuthentication.unBlacklistSASLMechanism(SASLPlainMechanism.NAME);

        } else {
            final Map<String, String> registeredSASLMechanisms = SASLAuthentication.getRegisterdSASLMechanisms();
            for (String mechanism : registeredSASLMechanisms.values()) {
                SASLAuthentication.unBlacklistSASLMechanism(mechanism);
            }
        }
    }

    private void connect() {
        try {
            xmppConnection.connect();
        } catch (SmackException e) {
            // There is no connection listeners yet, so we call onClose.
            LogManager.w(this, "Connection failed. SmackException " + e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            // There is no connection listeners yet, so we call onClose.
            LogManager.w(this, "Connection failed. IOException " + e.getMessage());
            throw new RuntimeException(e);
        } catch (XMPPException e) {
            // There is no connection listeners yet, so we call onClose.
            LogManager.w(this, "Connection failed. XMPPException " + e.getMessage());
            throw new RuntimeException(e);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onConnected();
            }
        });

        if (registerNewAccount) {
            registerAccount();
        } else {
            login();
        }
    }

    /**
     * Connection to the server has been established.
     *
     */
    private void onConnected() {
        connectionItem.onConnected(this);
        ConnectionManager.getInstance().onConnected(this);
    }

    /**
     * Register new account.
     */
    private void registerAccount() {
        try {
            AccountManager.getInstance(xmppConnection).createAccount(connectionSettings.getUserName(), connectionSettings.getPassword());
        } catch (SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
            LogManager.exception(connectionItem, e);
            connectionClosedOnError(e);
            // Server will destroy connection, but we can speedup
            // it.
            xmppConnection.disconnect();
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onAccountRegistered();
            }
        });

        login();
    }

    private void onAccountRegistered() {
        LogManager.i(this, "Account registered");
        connectionItem.onAccountRegistered(this);
    }

    private void login() {
        boolean success = false;

        try {
            xmppConnection.login(
                    connectionSettings.getUserName(),
                    connectionSettings.getPassword(),
                    connectionSettings.getResource());
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
                    connectionItem.onAuthFailed();
                }
            });
            connectionClosed();
        } catch (XMPPException e) {
            LogManager.exception(this, e);
            connectionClosedOnError(e);
        } catch (SmackException | IOException e) {
            LogManager.exception(this, e);
        }

        if (success) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onAuthorized();
                }
            });
        } else {
            xmppConnection.disconnect();
        }
    }

    /**
     * Authorization passed.
     */
    private void onAuthorized() {
        connectionItem.onAuthorized(this);
        ConnectionManager.getInstance().onAuthorized(this);
        if (connectionItem instanceof AccountItem) {
            com.xabber.android.data.account.AccountManager.getInstance().removeAuthorizationError(
                    ((AccountItem) connectionItem).getAccount());
        }
        shutdown();
    }

    @Override
    public void connected(XMPPConnection connection) {
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
    }

    @Override
    public void connectionClosed() {
        // Can be called on error, e.g. XMPPConnection#initConnection().
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionItem.onClose(ConnectionThread.this);
            }
        });
    }

    @Override
    public void connectionClosedOnError(final Exception e) {
        if (SettingsManager.showConnectionErrors()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(Application.getInstance(),
                            Application.getInstance().getString(R.string.CONNECTION_FAILED) + ": " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        }

        connectionClosed();
    }

    @Override
    public void reconnectingIn(int seconds) {
    }

    @Override
    public void reconnectionSuccessful() {
    }

    @Override
    public void reconnectionFailed(Exception e) {
    }

    @Override
    public void processPacket(final Stanza packet) throws SmackException.NotConnectedException {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ConnectionManager.getInstance().processPacket(ConnectionThread.this, packet);
            }
        });
    }

    @Override
    public void pingFailed() {
        LogManager.i(this, "pingFailed for " + getConnectionItem().getRealJid());
        getConnectionItem().forceReconnect();
    }

    /**
     * Filter to accept all packets.
     *
     * @author alexander.ivanov
     */
    static class AcceptAll implements StanzaFilter {
        @Override
        public boolean accept(Stanza packet) {
            return true;
        }
    }

    /**
     * Submit task to be executed in connection thread.
     *
     * @param runnable
     */
    private void runOnConnectionThread(final Runnable runnable) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (!connectionItem.isManaged(ConnectionThread.this))
                    return;
                try {
                    runnable.run();
                } catch (RuntimeException e) {
                    LogManager.exception(connectionItem, e);
                    connectionClosedOnError(e);
                }
            }
        });
    }

    /**
     * Commit changes received from connection thread in UI thread.
     *
     * @param runnable
     */
    private void runOnUiThread(final Runnable runnable) {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!connectionItem.isManaged(ConnectionThread.this))
                    return;
                runnable.run();
            }
        });
    }

}
