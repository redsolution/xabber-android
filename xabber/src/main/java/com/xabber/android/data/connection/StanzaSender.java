package com.xabber.android.data.connection;

import android.support.annotation.NonNull;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.sm.StreamManagementException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

public class StanzaSender {
    private static String LOG_TAG = StanzaSender.class.getSimpleName();

    /**
     * Send stanza to authenticated connection and add acknowledged listener if Stream Management is enabled on server.
     */
    public static void sendStanza(AccountJid account, Message stanza, StanzaListener acknowledgedListener) throws NetworkException {
        XMPPTCPConnection xmppConnection = getXmppTcpConnection(account);

        if (xmppConnection.isSmEnabled()) {
            try {
                xmppConnection.addStanzaIdAcknowledgedListener(stanza.getStanzaId(), acknowledgedListener);
            } catch (StreamManagementException.StreamManagementNotEnabledException e) {
                LogManager.exception(LOG_TAG, e);
            }
        }

        sendStanza(xmppConnection, stanza);
    }

    /**
     * Send stanza to authenticated connection.
     */
    public static void sendStanza(AccountJid account, Stanza stanza) throws NetworkException {
        sendStanza(getXmppTcpConnection(account), stanza);
    }

    private static void sendStanza(@NonNull XMPPTCPConnection xmppConnection, @NonNull Stanza stanza) throws NetworkException {
        if (!xmppConnection.isAuthenticated()) {
            LogManager.e(LOG_TAG, "sendStanza. Not connected! could not send stanza " + stanza);
            return;
        }

        try {
            xmppConnection.sendStanza(stanza);
        } catch (SmackException.NotConnectedException e) {
            throw new NetworkException(R.string.XMPP_EXCEPTION, e);
        } catch (InterruptedException e) {
            LogManager.exception(LOG_TAG, e);
        }
    }

    private static @NonNull XMPPTCPConnection getXmppTcpConnection(AccountJid account) throws NetworkException {
        AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem == null) {
            throw new NetworkException(R.string.NOT_CONNECTED);
        }

        XMPPTCPConnection returnConnection = accountItem.getConnection();
        if (!returnConnection.isAuthenticated()) {
            throw new NetworkException(R.string.NOT_CONNECTED);
        }
        return returnConnection;
    }
}
