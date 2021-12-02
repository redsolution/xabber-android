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

import android.util.Log;

import androidx.annotation.NonNull;

import com.xabber.android.data.account.AccountErrorEvent;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.extension.forward.ForwardComment;
import com.xabber.android.data.extension.forward.ForwardCommentProvider;
import com.xabber.android.data.extension.httpfileupload.CustomDataProvider;
import com.xabber.android.data.extension.references.ReferenceElement;
import com.xabber.android.data.extension.references.ReferencesProvider;
import com.xabber.android.data.extension.devices.DevicesManager;
import com.xabber.android.data.log.AndroidLoggingHandler;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.xaccount.HttpConfirmIq;
import com.xabber.android.data.xaccount.HttpConfirmIqProvider;
import com.xabber.xmpp.groups.rights.GroupchatMemberRightsReplyIQ;
import com.xabber.xmpp.groups.rights.GroupchatMemberRightsReplyIqProvider;
import com.xabber.xmpp.smack.SaslHtopMechanism;
import com.xabber.xmpp.smack.XMPPTCPConnection;
import com.xabber.xmpp.devices.IncomingNewDeviceIQ;
import com.xabber.xmpp.devices.ResultSessionsIQ;
import com.xabber.xmpp.devices.providers.SessionsProvider;
import com.xabber.xmpp.devices.providers.DeviceProvider;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import java.io.IOException;
import java.util.logging.Level;

import de.measite.minidns.AbstractDNSClient;

class ConnectionThread {

    @NonNull
    private final XMPPTCPConnection connection;
    @SuppressWarnings("WeakerAccess")
    @NonNull
    final ConnectionItem connectionItem;
    private Thread thread;

    ConnectionThread(@NonNull XMPPTCPConnection connection, @NonNull ConnectionItem connectionItem) {
        this.connection = connection;
        this.connectionItem = connectionItem;
        createNewThread();
    }

    private void createNewThread() {
        LogManager.i(this, "Creating new connection thread");
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (NetworkManager.isNetworkAvailable()) {
                    connectAndLogin();
                } else {
                    connectionItem.updateState(ConnectionState.waiting);
                    LogManager.i(this, "No network connection");
                }
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
    }

    /**
     *
     * @return true if connection thread started, false if already running - nothing changed
     */
    boolean start() {
        if (thread.getState() == Thread.State.TERMINATED) {
            LogManager.i(this, "Connection thread is finished, creating new one...");
            createNewThread();
        }

        if (thread.getState() == Thread.State.NEW) {
            LogManager.i(this, "Connection thread is new, starting...");
            thread.start();
            return true;
        } else {
            LogManager.i(this, "Connection thread is running already");
            return false;
        }
    }

    @SuppressWarnings("WeakerAccess")
    void connectAndLogin() {
        AndroidLoggingHandler.reset(new AndroidLoggingHandler());
        java.util.logging.Logger.getLogger(XMPPTCPConnection.class.getName()).setLevel(Level.ALL);
        java.util.logging.Logger.getLogger(AbstractDNSClient.class.getName()).setLevel(Level.ALL);
        java.util.logging.Logger.getLogger(AbstractXMPPConnection.class.getName()).setLevel(Level.ALL);
        java.util.logging.Logger.getLogger(DNSUtil.class.getName()).setLevel(Level.ALL);

        if (connection.getConfiguration().getPassword().isEmpty()) {
            AccountErrorEvent accountErrorEvent = new AccountErrorEvent(connectionItem.getAccount(),
                    AccountErrorEvent.Type.PASS_REQUIRED, "");

            com.xabber.android.data.account.AccountManager.INSTANCE.setEnabled(connectionItem.getAccount(), false);
            EventBus.getDefault().postSticky(accountErrorEvent);
            return;
        }

        LogManager.i(this, "Use DNS Java resolver");
        ExtDNSJavaResolver.setup();

        ProviderManager.addExtensionProvider(
                DataForm.ELEMENT,
                DataForm.NAMESPACE,
                new CustomDataProvider()
        );

        ProviderManager.addExtensionProvider(
                ForwardComment.ELEMENT,
                ForwardComment.NAMESPACE,
                new ForwardCommentProvider()
        );

        ProviderManager.addExtensionProvider(
                ReferenceElement.ELEMENT,
                ReferenceElement.NAMESPACE,
                new ReferencesProvider()
        );

        ProviderManager.addIQProvider(
                IncomingNewDeviceIQ.ELEMENT,
                IncomingNewDeviceIQ.NAMESPACE,
                new DeviceProvider()
        );

        ProviderManager.addIQProvider(
                ResultSessionsIQ.ELEMENT,
                ResultSessionsIQ.NAMESPACE,
                new SessionsProvider()
        );

        ProviderManager.addIQProvider(
                GroupchatMemberRightsReplyIQ.ELEMENT,
                GroupchatMemberRightsReplyIQ.NAMESPACE + GroupchatMemberRightsReplyIQ.HASH_BLOCK,
                new GroupchatMemberRightsReplyIqProvider()
        );

        try {
            LogManager.i(this, "Trying to connect and login...");
            if (!connection.isConnected()) {
                connectionItem.updateState(ConnectionState.connecting);
                connection.connect();
            } else {
                LogManager.i(this, "Already connected");
            }

            if (!connection.isAuthenticated()) {
                ProviderManager.addIQProvider(HttpConfirmIq.ELEMENT,
                        HttpConfirmIq.NAMESPACE, new HttpConfirmIqProvider());

                if (connectionItem.getConnectionSettings().getDevice() != null) {
                    //DevicesManager.INSTANCE.beforeLogin(connectionItem);
                    connection.login(
                            connectionItem.getConnectionSettings().getUserName(),
                            connectionItem.getConnectionSettings().getDevice().getPasswordString()
                    );
                } else {
                    connection.login();
                }

                ((AccountItem)connectionItem).setStreamError(false);
            } else {
                LogManager.i(this, "Already authenticated");
            }
        } catch (SASLErrorException e)  {
            LogManager.exception(this, e);

            if (e.getMechanism().equals(SaslHtopMechanism.NAME)) {
                switch (e.getSASLFailure().getSASLError()) {
                    case credentials_expired: {
                        DevicesManager.INSTANCE.onAccountDeviceRevokedOrExpired(connectionItem.getAccount());
                        break;
                    }
                    case not_authorized: {
                        DevicesManager.INSTANCE.onPasswordIncorrect(connectionItem.getAccount());
                        break;
                    }
                }
            } else {
                AccountErrorEvent accountErrorEvent = new AccountErrorEvent(
                        connectionItem.getAccount(),
                        AccountErrorEvent.Type.AUTHORIZATION,
                        e.getMessage()
                );

                com.xabber.android.data.account.AccountManager.INSTANCE.setEnabled(
                        connectionItem.getAccount(), false
                );
                EventBus.getDefault().postSticky(accountErrorEvent);
            }
        } catch (XMPPException | SmackException | IOException | RuntimeException e) {
            LogManager.exception(this, e);

            if (!((AccountItem)connectionItem).isSuccessfulConnectionHappened()) {
                LogManager.i(this, "There was no successful connection, disabling account");

                AccountErrorEvent accountErrorEvent;
                if (e instanceof XMPPException.StreamErrorException) {
                    accountErrorEvent = new AccountErrorEvent(
                            connectionItem.getAccount(),
                            AccountErrorEvent.Type.CONNECTION,
                            ((XMPPException.StreamErrorException)e).getStreamError().getDescriptiveText()
                    );
                } else {
                    accountErrorEvent = new AccountErrorEvent(
                            connectionItem.getAccount(),
                            AccountErrorEvent.Type.CONNECTION,
                            Log.getStackTraceString(e)
                    );
                }

                com.xabber.android.data.account.AccountManager.INSTANCE.addAccountError(accountErrorEvent);
                if (e instanceof XMPPException.StreamErrorException) {
                    ((AccountItem)connectionItem).setStreamError(true);
                } else {
                    if (!((AccountItem)connectionItem).getStreamError()) {
                        com.xabber.android.data.account.AccountManager.INSTANCE.setEnabled(connectionItem.getAccount(), false);
                    }
                }
                EventBus.getDefault().postSticky(accountErrorEvent);
            }
        } catch (InterruptedException e) {
            LogManager.exception(this, e);
        }
        LogManager.i(this, "Connection thread finished");
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + connectionItem.getAccount();
    }

}
