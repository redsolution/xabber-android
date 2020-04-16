package com.xabber.android.data.connection;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.listeners.OnConnectedListener;
import com.xabber.android.data.connection.listeners.OnDisconnectListener;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.bookmarks.BookmarksManager;
import com.xabber.android.data.extension.carbons.CarbonManager;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.rrr.RrrManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.ChatManager;
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
        connectionItem.refreshPingFailedListener(true);

        // just to see the order of call
        CarbonManager.getInstance().onAuthorized(connectionItem);
        LogManager.i(getLogTag(), "finished carbonManger onAuthorized");
        BlockingManager.getInstance().onAuthorized(connectionItem);
        LogManager.i(getLogTag(), "finished blockingManager onAuthorized");
        HttpFileUploadManager.getInstance().onAuthorized(connectionItem);
        LogManager.i(getLogTag(), "finished httpFile onAuthorized");
        PresenceManager.getInstance().onAuthorized(connectionItem);
        LogManager.i(getLogTag(), "finished presenceManager onAuthorized");
        BookmarksManager.getInstance().onAuthorized(connectionItem.getAccount());
        LogManager.i(getLogTag(), "finished bookmarksManager onAuthorized");
        RrrManager.getInstance().subscribeForUpdates();
        LogManager.i(getLogTag(), "finished rrrManager onAuthorized");
        //UserAvatarManager.getInstanceFor(connection).onAuthorized();
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
        PresenceManager.getInstance().clearPresencesTiedToThisAccount(connectionItem.getAccount());
        connectionItem.updateState(ConnectionState.offline);

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionItem.checkIfConnectionIsOutdated();
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
        PresenceManager.getInstance().clearPresencesTiedToThisAccount(connectionItem.getAccount());
        connectionItem.updateState(ConnectionState.waiting);
        connectionItem.refreshPingFailedListener(false);

        if (e instanceof XMPPException.StreamErrorException) {
            LogManager.e(getLogTag(), e.getMessage());
            String message = e.getMessage();
            if (message != null && message.contains("conflict")) {
                AccountManager.getInstance().generateNewResourceForAccount(connectionItem.getAccount());
            } else {
                ((AccountItem)connectionItem).setStreamError(true);
            }
        }

        if (e instanceof SASLErrorException) {
            AccountManager.getInstance().setEnabled(connectionItem.getAccount(), false);
        }

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionItem.checkIfConnectionIsOutdated();
                /*
                  Send to chats action of disconnect
                  Then RoomChat set state in "waiting" which need for rejoin to room
                 */
                ChatManager.getInstance().onDisconnect(connectionItem);
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
