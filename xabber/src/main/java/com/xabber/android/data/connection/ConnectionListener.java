package com.xabber.android.data.connection;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.listeners.OnAuthorizedListener;
import com.xabber.android.data.connection.listeners.OnConnectedListener;
import com.xabber.android.data.connection.listeners.OnDisconnectListener;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.RosterManager;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.sasl.SASLErrorException;

class ConnectionListener implements org.jivesoftware.smack.ConnectionListener {

    @SuppressWarnings("WeakerAccess")
    ConnectionItem connectionItem;
    private String LOG_TAG = ConnectionListener.class.getSimpleName();

    ConnectionListener(ConnectionItem connectionItem) {
        this.connectionItem = connectionItem;
    }

    @Override
    public void connected(XMPPConnection connection) {
        LogManager.i(LOG_TAG, "connected");

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionItem.showDebugToast("connected");
                connectionItem.updateState(ConnectionState.authentication);

                for (OnConnectedListener listener : Application.getInstance().getManagers(OnConnectedListener.class)) {
                    listener.onConnected(connectionItem);
                }
            }
        });
    }

    @Override
    public void authenticated(XMPPConnection connection, final boolean resumed) {
        LogManager.i(LOG_TAG, "authenticated. resumed: " + resumed);

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (resumed) {
                    connectionItem.showDebugToast("authenticated resumed");
                } else {
                    connectionItem.showDebugToast("authenticated");
                }

                connectionItem.updateState(ConnectionState.connected);

                if (resumed) {
                    RosterManager.getInstance().updateContacts();
                }

                for (OnAuthorizedListener listener : Application.getInstance().getManagers(OnAuthorizedListener.class)) {
                    listener.onAuthorized(connectionItem);
                }
                AccountManager.getInstance().removeAuthorizationError(connectionItem.getAccount());
            }
        });
    }

    @Override
    public void connectionClosed() {
        LogManager.i(LOG_TAG, "connectionClosed");

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionItem.showDebugToast("connection closed");

                connectionItem.updateState(ConnectionState.offline);

                for (OnDisconnectListener listener
                        : Application.getInstance().getManagers(OnDisconnectListener.class)) {
                    listener.onDisconnect(connectionItem);
                }
            }
        });
    }

    // going to reconnect with Smack Reconnection manager
    @Override
    public void connectionClosedOnError(final Exception e) {
        LogManager.i(LOG_TAG, "connectionClosedOnError " + e + " " + e.getMessage());

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionItem.showDebugToast("connection closed on error: " + e.getMessage() + ". Exception: " + e.getClass().getSimpleName());
                connectionItem.updateState(ConnectionState.waiting);

                if (e instanceof SASLErrorException) {
                    connectionItem.showDebugToast("Auth error!");
                    AccountManager.getInstance().setEnabled(connectionItem.getAccount(), false);
                }
            }
        });
    }

    @Override
    public void reconnectionSuccessful() {
        LogManager.i(LOG_TAG, "reconnectionSuccessful");

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionItem.showDebugToast("reconnection successful");
            }
        });
    }

    @Override
    public void reconnectingIn(final int seconds) {
        LogManager.i(LOG_TAG, "reconnectionSuccessful");

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionItem.showDebugToast("reconnecting in " + seconds + " seconds");

                if (connectionItem.getState() != ConnectionState.waiting && !connectionItem.getConnection().isAuthenticated()
                        && !connectionItem.getConnection().isConnected()) {
                    connectionItem.updateState(ConnectionState.waiting);
                }
            }
        });
    }

    @Override
    public void reconnectionFailed(final Exception e) {
        LogManager.i(LOG_TAG, "reconnectionFailed " + e + " " + e.getMessage());

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionItem.showDebugToast("reconnection failed: " + e.getMessage() + ". Exception: " + e.getClass().getSimpleName());
                connectionItem.updateState(ConnectionState.offline);
            }
        });
    }
}
