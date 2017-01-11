package com.xabber.android.data.connection;

import android.widget.Toast;

import com.xabber.android.data.Application;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.listeners.OnAuthorizedListener;
import com.xabber.android.data.connection.listeners.OnConnectedListener;
import com.xabber.android.data.connection.listeners.OnDisconnectListener;
import com.xabber.android.data.roster.RosterManager;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.sasl.SASLErrorException;

class ConnectionListener implements org.jivesoftware.smack.ConnectionListener {

    private ConnectionItem connectionItem;
    private String LOG_TAG = ConnectionListener.class.getSimpleName();

    ConnectionListener(ConnectionItem connectionItem) {
        this.connectionItem = connectionItem;
    }

    @Override
    public void connected(XMPPConnection connection) {
        LogManager.i(LOG_TAG, "connected");

        connectionItem.showDebugToast("connected");

        connectionItem.updateState(ConnectionState.authentication);

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (OnConnectedListener listener : Application.getInstance().getManagers(OnConnectedListener.class)) {
                    listener.onConnected(connectionItem);
                }
            }
        });
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        LogManager.i(LOG_TAG, "authenticated. resumed: " + resumed);

        if (resumed) {
            connectionItem.showDebugToast("authenticated resumed");
        } else {
            connectionItem.showDebugToast("authenticated");
        }

        connectionItem.updateState(ConnectionState.connected);

        if (resumed) {
            RosterManager.getInstance().updateContacts();
        }

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
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

        connectionItem.showDebugToast("connection closed");

        connectionItem.updateState(ConnectionState.offline);

        for (OnDisconnectListener listener : Application.getInstance().getManagers(OnDisconnectListener.class)) {
            listener.onDisconnect(connectionItem);
        }
    }

    // going to reconnect with Smack Reconnection manager
    @Override
    public void connectionClosedOnError(final Exception e) {
        LogManager.i(LOG_TAG, "connectionClosedOnError " + e + " " + e.getMessage());

        connectionItem.showDebugToast("connection closed on error: " + e.getMessage() + ". Exception: " + e.getClass().getSimpleName());
        connectionItem.updateState(ConnectionState.waiting);

        if (e instanceof SASLErrorException) {
            Application.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectionItem.showDebugToast("Auth error!");
                    AccountManager.getInstance().setEnabled(connectionItem.getAccount(), false);
                }
            });
            return;
        }

        if (e instanceof XMPPException.StreamErrorException) {
            LogManager.i(this, "Stream error.");
            connectionItem.createNewConnection();
        }
    }

    @Override
    public void reconnectionSuccessful() {
        LogManager.i(LOG_TAG, "reconnectionSuccessful");

        connectionItem.showDebugToast("reconnection successful");
    }

    @Override
    public void reconnectingIn(int seconds) {
        LogManager.i(LOG_TAG, "reconnectionSuccessful");

        connectionItem.showDebugToast("reconnecting in " + seconds + " seconds", Toast.LENGTH_SHORT);

        if (connectionItem.getState() != ConnectionState.waiting && !connectionItem.getConnection().isAuthenticated()
                && !connectionItem.getConnection().isConnected()) {
            connectionItem.updateState(ConnectionState.waiting);
        }
    }

    @Override
    public void reconnectionFailed(Exception e) {
        LogManager.i(LOG_TAG, "reconnectionFailed " + e + " " + e.getMessage());

        connectionItem.showDebugToast("reconnection failed: " + e.getMessage() + ". Exception: " + e.getClass().getSimpleName());
        connectionItem.updateState(ConnectionState.offline);
    }
}
