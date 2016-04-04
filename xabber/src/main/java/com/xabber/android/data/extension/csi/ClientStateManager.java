package com.xabber.android.data.extension.csi;

import com.xabber.android.data.LogManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionThread;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smackx.csi.packet.ClientStateIndication;

/**
 * Client State Indication (XEP-0352) manager
 * @author Ricki Hirner (www.bitfire.at)
 */
public class ClientStateManager {

    private static final String LOG_TAG = ClientStateManager.class.getSimpleName();

    private ClientStateManager() {
    }


    public static void setInactive() {
        sendClientState(ClientStateIndication.Inactive.INSTANCE);
    }

    public static void setActive() {
        sendClientState(ClientStateIndication.Active.INSTANCE);
    }

    protected static void sendClientState(Nonza nonza) {
        AccountManager accountManager = AccountManager.getInstance();
        for (String accountName : accountManager.getAccounts()) {
            AccountItem account = accountManager.getAccount(accountName);
            if (account == null) {
                continue;
            }
            ConnectionThread connectionThread = account.getConnectionThread();
            if (connectionThread == null) {
                continue;
            }

            AbstractXMPPConnection xmppConnection = connectionThread.getXMPPConnection();

            if (xmppConnection.hasFeature("csi", ClientStateIndication.NAMESPACE))
                try {
                    xmppConnection.sendNonza(nonza);
                } catch (SmackException.NotConnectedException | InterruptedException e) {
                    LogManager.exception(LOG_TAG, e);
                }
        }
    }

}
