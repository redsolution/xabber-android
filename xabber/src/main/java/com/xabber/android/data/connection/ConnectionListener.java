package com.xabber.android.data.connection;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.listeners.OnConnectedListener;
import com.xabber.android.data.connection.listeners.OnDisconnectListener;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.bookmarks.BookmarksManager;
import com.xabber.android.data.extension.carbons.CarbonManager;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.xmpp.avatar.UserAvatarManager;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.sasl.SASLErrorException;

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

        //
        UserAvatarManager.getInstanceFor(connection).enable();
        //

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
    public void authenticated(XMPPConnection connection, final boolean resumed) {
        LogManager.i(getLogTag(), "authenticated. resumed: " + resumed);
        connectionItem.updateState(ConnectionState.connected);

        // just to see the order of call
        CarbonManager.getInstance().onAuthorized(connectionItem);
        BlockingManager.getInstance().onAuthorized(connectionItem);
        HttpFileUploadManager.getInstance().onAuthorized(connectionItem);
        PresenceManager.getInstance().onAuthorized(connectionItem);
        BookmarksManager.getInstance().onAuthorized(connectionItem.getAccount());

        //
        //

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AccountManager.getInstance().removeAccountError(connectionItem.getAccount());
            }
        });
    }

    @Override
    public void connectionClosed() {
        LogManager.i(getLogTag(), "connectionClosed");
        connectionItem.updateState(ConnectionState.offline);

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
        LogManager.i(getLogTag(), "connectionClosedOnError " + e + " " + e.getMessage());
        connectionItem.updateState(ConnectionState.waiting);

        if (e instanceof XMPPException.StreamErrorException) {
            String message = e.getMessage();
            if (message.contains("conflict")) {
                AccountManager.getInstance().generateNewResourceForAccount(connectionItem.getAccount());
            }
        }

        if (e instanceof SASLErrorException) {
            AccountManager.getInstance().setEnabled(connectionItem.getAccount(), false);
        }

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                /*
                  Send to chats action of disconnect
                  Then RoomChat set state in "waiting" which need for rejoin to room
                 */
                MessageManager.getInstance().onDisconnect(connectionItem);
            }
        });
    }

    @Override
    public void reconnectionSuccessful() {
        LogManager.i(getLogTag(), "reconnectionSuccessful");
    }

    @Override
    public void reconnectingIn(final int seconds) {
        LogManager.i(getLogTag(), "reconnectionSuccessful");
        if (connectionItem.getState() != ConnectionState.waiting && !connectionItem.getConnection().isAuthenticated()
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
