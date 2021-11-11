package com.xabber.android.data.connection;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.log.LogManager;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

class ConnectionListener implements org.jivesoftware.smack.ConnectionListener {

    @SuppressWarnings("WeakerAccess")
    ConnectionItem connectionItem;

    ConnectionListener(ConnectionItem connectionItem) {
        this.connectionItem = connectionItem;
    }

    private String getLogTag() {
        StringBuilder logTag = new StringBuilder();
        logTag.append(getClass().getSimpleName());

        if (connectionItem != null) {
            logTag.append(": ");
            logTag.append(connectionItem.getAccount());
        }
        return logTag.toString();
    }

    @Override
    public void connected(XMPPConnection connection) {
        LogManager.i(getLogTag(), "connected");
        connectionItem.updateState(ConnectionState.authentication);

        Application.getInstance().runOnUiThread(() -> {
            for (OnConnectedListener listener : Application.getInstance().getManagers(OnConnectedListener.class)) {
                listener.onConnected(connectionItem);
            }
        });
    }

    @Override
    public void authenticated(XMPPConnection connection, final boolean resumed) {
        LogManager.i(getLogTag(), "authenticated. resumed: " + resumed);
        connectionItem.updateState(ConnectionState.connected);
        connectionItem.refreshPingFailedListener(true);

        try {
            ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(connection.getXMPPServiceDomain());
        } catch (SmackException.NoResponseException |
                XMPPException.XMPPErrorException |
                SmackException.NotConnectedException |
                InterruptedException e) {
            LogManager.exception(getClass().getSimpleName(), e);
        }
        LogManager.i(getLogTag(), "finished discovering and saving server info");

        for (OnAuthenticatedListener listener : Application.getInstance().getManagers(OnAuthenticatedListener.class)){
            listener.onAuthenticated(connectionItem);
        }

    }

    @Override
    public void connectionClosed() {
        LogManager.i(getLogTag(), "connectionClosed");
        connectionItem.updateState(ConnectionState.offline);

        Application.getInstance().runOnUiThread(() -> {
            connectionItem.checkIfConnectionIsOutdated();
            for (OnDisconnectListener listener : Application.getInstance().getManagers(OnDisconnectListener.class)) {
                listener.onDisconnect(connectionItem);
            }
        });
    }

    // going to reconnect with Smack Reconnection manager
    @Override
    public void connectionClosedOnError(final Exception e) {
        LogManager.i(getLogTag(), "connectionClosedOnError " + e + " " + e.getMessage());

        connectionItem.updateState(ConnectionState.waiting);
        connectionItem.refreshPingFailedListener(false);

        if (e instanceof XMPPException.StreamErrorException) {
            LogManager.e(getLogTag(), e.getMessage());
            String message = e.getMessage();
            if (message != null && message.contains("conflict")) {
                AccountManager.INSTANCE.generateNewResourceForAccount(connectionItem.getAccount());
            } else ((AccountItem)connectionItem).setStreamError(true);
        }

        if (e instanceof SASLErrorException) {
            AccountManager.INSTANCE.setEnabled(connectionItem.getAccount(), false);
        }

        Application.getInstance().runOnUiThread(() -> connectionItem.checkIfConnectionIsOutdated());

        for (OnDisconnectListener listener : Application.getInstance().getManagers(OnDisconnectListener.class)){
            listener.onDisconnect(connectionItem);
        }
    }

    @Override
    public void reconnectionSuccessful() {
        LogManager.i(getLogTag(), "reconnectionSuccessful");
    }

    @Override
    public void reconnectingIn(final int seconds) {
        LogManager.i(getLogTag(), "reconnectionSuccessful");
        if (connectionItem.getState() != ConnectionState.waiting
                && !connectionItem.getConnection().isAuthenticated()
                && !connectionItem.getConnection().isConnected()) {
            connectionItem.updateState(ConnectionState.waiting);
        }
    }

    @Override
    public void reconnectionFailed(final Exception e) {
        LogManager.i(getLogTag(), "reconnectionFailed " + e + " " + e.getMessage());
        connectionItem.updateState(ConnectionState.offline);
    }

}
