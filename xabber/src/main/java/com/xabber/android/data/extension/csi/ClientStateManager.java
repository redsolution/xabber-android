package com.xabber.android.data.extension.csi;

import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;

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
        for (AccountJid accountName : accountManager.getEnabledAccounts()) {
            AccountItem accountItem = accountManager.getAccount(accountName);
            if (accountItem == null) {
                continue;
            }

            AbstractXMPPConnection xmppConnection = accountItem.getConnection();
            if (!xmppConnection.isAuthenticated()) {
                continue;
            }

            if (xmppConnection.hasFeature("csi", ClientStateIndication.NAMESPACE))
                try {
                    xmppConnection.sendNonza(nonza);
                } catch (SmackException.NotConnectedException | InterruptedException e) {
                    LogManager.exception(LOG_TAG, e);
                }
        }
    }

}
