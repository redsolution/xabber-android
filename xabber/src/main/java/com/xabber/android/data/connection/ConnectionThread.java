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
import com.xabber.android.data.log.LogManager;

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
                connectAndLogin();
            }
        }, "Connection thread for " + connectionItem.getAccount());
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
    }

    void start() {
        if (thread.getState() == Thread.State.TERMINATED) {
            LogManager.i(this, "Connection thread is finished, creating new one...");
            createNewThread();
        }

        if (thread.getState() == Thread.State.NEW) {
            LogManager.i(this, "Connection thread is new, starting...");
            thread.start();
        } else {
            LogManager.i(this, "Connection thread is running already");
        }
    }

    private void connectAndLogin() {
        try {
            LogManager.i(this, "Trying to connect and login...");
            connection.connect().login();
        } catch (SASLErrorException e)  {
            LogManager.exception(this, e);
            LogManager.i(this, "Error. " + e.getMessage() + " Exception class: " + e.getClass().getSimpleName());
            Application.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectionItem.showDebugToast("Auth error!");
                    com.xabber.android.data.account.AccountManager.getInstance().setEnabled(connectionItem.getAccount(), false);
                }
            });
        } catch (XMPPException | SmackException | IOException | InterruptedException e) {
            LogManager.exception(this, e);
        } catch (NullPointerException | IllegalArgumentException e) {
            // TODO - should be fixed either in Smack or Xabber
            LogManager.exception(this, e);
        }
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
//            connectionItem.onAccountRegistered();
        }

        return success;
    }
}
