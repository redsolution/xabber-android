package com.xabber.android.data.extension.carbons;

import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.SettingsManager.SecurityOtrMode;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.hints.element.NoStoreHint;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Packet extension for XEP-0280: Message Carbons. This class implements
 * the manager for registering Carbon support, enabling and disabling
 * message carbons.
 * <p/>
 * You should call enableCarbons() before sending your first undirected
 * presence.
 *
 * @author Georg Lukas, Semyon Baranov
 */
public class CarbonManager {

    private static CarbonManager instance;
    private static final String LOG_TAG = CarbonManager.class.getSimpleName();

    @SuppressWarnings("WeakerAccess")
    Map<AccountJid, CarbonCopyListener> carbonCopyListeners;

    public static CarbonManager getInstance() {
        if (instance == null) {
            instance = new CarbonManager();
        }
        return instance;
    }

    private CarbonManager() {
        carbonCopyListeners = new ConcurrentHashMap<>();
    }

    public void onAuthorized(final ConnectionItem connection) {
        updateIsSupported(connection);
    }

    @SuppressWarnings("WeakerAccess")
    void updateIsSupported(final ConnectionItem connectionItem) {

        org.jivesoftware.smackx.carbons.CarbonManager carbonManager
                = org.jivesoftware.smackx.carbons.CarbonManager
                .getInstanceFor(connectionItem.getConnection());

        try {
            if (connectionItem.getConnection() != null && connectionItem.getConnection().getUser() != null && carbonManager.isSupportedByServer()) {
                LogManager.d(LOG_TAG, "Smack reports that carbons are " + (carbonManager.getCarbonsEnabled() ? "enabled" : "disabled"));
                if (carbonManager.getCarbonsEnabled()) {
                    // Sometimes Smack's CarbonManager still thinks that carbons are enabled during a
                    // period of time between disconnecting on error and completing a new authorization.
                    // Since our onAuthorized listener could be called earlier than the listener in Smack's CarbonManager,
                    // it can introduce an incorrect behavior of .getCarbonsEnabled().
                    // To avoid it we can use .enableCarbonsAsync(), and its' counterpart, to skip the Carbons's
                    // state check and "forcefully" send the correct carbons state IQ
                    changeCarbonsStateAsync(carbonManager, connectionItem.getAccount(), SettingsManager.connectionUseCarbons());
                } else {
                    changeCarbonsState(carbonManager, connectionItem.getAccount(), SettingsManager.connectionUseCarbons());
                }
            }
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                | SmackException.NotConnectedException | InterruptedException e) {
            LogManager.exception(this, e);
            if (e instanceof SmackException.NoResponseException) {
                if (SettingsManager.connectionUseCarbons()) {
                    addListener(carbonManager, connectionItem.getAccount());
                } else {
                    removeListener(carbonManager, connectionItem.getAccount());
                }
            }
        }
    }

    // Async method sends carbons IQ without checking the current state, which is useful when the order of
    // authorized listeners becomes messed up and Smack's Carbons state flag doesn't reflect the real state since it didn't update yet.
    private void changeCarbonsStateAsync(org.jivesoftware.smackx.carbons.CarbonManager carbonManager, AccountJid account, boolean enable)
            throws InterruptedException {
        if (enable) {
            carbonManager.enableCarbonsAsync(null);
            addListener(carbonManager, account);
            LogManager.d(LOG_TAG, "Forcefully sent <enable> carbons");
        } else {
            carbonManager.disableCarbonsAsync(null);
            removeListener(carbonManager, account);
            LogManager.d(LOG_TAG, "Forcefully sent <disable> carbons");
        }
    }

    private void changeCarbonsState(org.jivesoftware.smackx.carbons.CarbonManager carbonManager, AccountJid account, boolean enable)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        if (enable) {
            carbonManager.setCarbonsEnabled(true);
            addListener(carbonManager, account);
        } else {
            carbonManager.setCarbonsEnabled(false);
            removeListener(carbonManager, account);
        }
        LogManager.d(LOG_TAG, "Tried to send normal settings dependent carbons = " +
                (enable ? "enabled" : "disabled"));
    }

    // we need to remove old listener not to cause memory leak
    private void addListener(org.jivesoftware.smackx.carbons.CarbonManager carbonManager, AccountJid account) {
        removeListener(carbonManager, account);

        CarbonCopyListener carbonCopyListener = new CarbonCopyListener(account);
        carbonCopyListeners.put(account, carbonCopyListener);
        carbonManager.addCarbonCopyReceivedListener(carbonCopyListener);
    }

    private void removeListener(org.jivesoftware.smackx.carbons.CarbonManager carbonManager, AccountJid account) {
        CarbonCopyListener carbonCopyListener = carbonCopyListeners.remove(account);
        if (carbonCopyListener != null) {
            carbonManager.removeCarbonCopyReceivedListener(carbonCopyListener);
        }
    }

    public boolean isCarbonsEnabledForConnection(ConnectionItem connection) {
        return org.jivesoftware.smackx.carbons.CarbonManager
                .getInstanceFor(connection.getConnection())
                .getCarbonsEnabled();
    }

    public boolean isSupportedByServer(ConnectionItem connection) throws XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        return org.jivesoftware.smackx.carbons.CarbonManager
                .getInstanceFor(connection.getConnection())
                .isSupportedByServer();
    }

    /**
     * Sends the new state of message carbons to the server
     * when this setting has been changed
     */
    public void onUseCarbonsSettingsChanged() {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                Collection<AccountJid> accounts = AccountManager.getInstance().getEnabledAccounts();
                for (AccountJid account : accounts) {
                    updateIsSupported(AccountManager.getInstance().getAccount(account));
                }
            }
        });
    }

    /**
     * Update outgoing message before sending.
     * Marks the message as non-carbon-copied in the following cases:
     * - Message Carbons is enabled and OTR mode is enabled.
     * - Message Carbons is enabled and OTR security level != plain.
     *
     * @param abstractChat
     * @param message      the <tt>Message</tt> to be sent
     */
    public void updateOutgoingMessage(AbstractChat abstractChat, Message message) {
        if (!SettingsManager.connectionUseCarbons()) {
            return;
        }

        if (SettingsManager.securityOtrMode() == SecurityOtrMode.disabled) {
            return;
        }

        SecurityLevel securityLevel = OTRManager.getInstance().getSecurityLevel(abstractChat.getAccount(), abstractChat.getUser());
        if (securityLevel == SecurityLevel.plain || securityLevel == SecurityLevel.finished) {
            return;
        }

//        if (isCarbonsEnabledForConnection(AccountManager.getInstance().getAccount(abstractChat.getAccount()))) {
//            return;
//        }

        CarbonExtension.Private.addTo(message);
    }

    /**
     * Marks the message as non-carbon-copied
     * Should used for establishing OTR-session
     * @param message      the <tt>Message</tt> to be sent
     */
    public void setMessageToIgnoreCarbons(Message message) {
        CarbonExtension.Private.addTo(message);
        NoStoreHint.set(message);
    }
}