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

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.iqregister.AccountManager;

import java.io.IOException;

/**
 * Provides connection workflow.
 *
 * @author alexander.ivanov
 */
class ConnectionThread {

    @NonNull
    private final XMPPTCPConnection connection;
    @NonNull
    private final ConnectionItem connectionItem;

    ConnectionThread(@NonNull XMPPTCPConnection connection, @NonNull ConnectionItem connectionItem) {
        this.connection = connection;
        this.connectionItem = connectionItem;
    }

    void start(final boolean registerNewAccount) {
        LogManager.i(this, "start. registerNewAccount " + registerNewAccount);

        Thread thread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        boolean success = connect();
                        if (success && registerNewAccount) {
                            success = createAccount();
                        }

                        if (success) {
                            success = login();
                        }

                        if (!success) {
                            connection.disconnect();
                        }
                    }
                },
                "Connection thread for " + connectionItem.getRealJid());
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    private boolean connect() {
        boolean success = false;

        try {
            connection.connect();
            success = true;
        } catch (SmackException e) {
            // There is no connection listeners yet, so we call onClose.

            LogManager.exception(this, e);

            if (e instanceof SmackException.ConnectionException) {
                LogManager.w(this, "Connection failed. SmackException.ConnectionException " + e.getMessage());
            } else {
                LogManager.w(this, "Connection failed. SmackException " + e.getMessage());
            }

            connectionItem.connectionClosed();
        } catch (IOException e) {
            // There is no connection listeners yet, so we call onClose.
            LogManager.w(this, "Connection failed. IOException " + e.getMessage());
            LogManager.exception(this, e);
            connectionItem.connectionClosed();
        } catch (XMPPException e) {
            // There is no connection listeners yet, so we call onClose.
            LogManager.w(this, "Connection failed. XMPPException " + e.getMessage());
            LogManager.exception(this, e);
            connectionItem.connectionClosed();
        } catch (InterruptedException e) {
            LogManager.w(this, "Connection failed. InterruptedException " + e.getMessage());
            LogManager.exception(this, e);
            connectionItem.connectionClosed();
        }
        return success;
    }

    private boolean createAccount() {
        boolean success = false;
        try {
            AccountManager.getInstance(connection)
                    .createAccount(connectionItem.getConnectionSettings().getUserName(),
                            connectionItem.getConnectionSettings().getPassword());
            success = true;
        } catch (SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException | InterruptedException e) {
            LogManager.exception(this, e);
        }

        if (success) {
            connectionItem.onAccountRegistered();
        }

        return success;
    }

    private boolean login() {
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
                    connectionItem.onAuthFailed();
                }
            });
            connectionItem.connectionClosed();
        } catch (XMPPException e) {
            LogManager.exception(this, e);
            connectionItem.connectionClosed();
        } catch (SmackException | IOException | InterruptedException e) {
            LogManager.exception(this, e);
        }

        return success;
    }


}
