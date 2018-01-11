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
            if (carbonManager.isSupportedByServer()) {
                if (carbonManager.getCarbonsEnabled()) {
                    // Smack CarbonManager still thinks, that carbons enabled and does not sent IQ
                    // it drops flag to false when on authorized listener, but it happens after this listener
                    // so it is problem of unordered authorized listeners
                    carbonManager.setCarbonsEnabled(false);
                }
                carbonManager.setCarbonsEnabled(SettingsManager.connectionUseCarbons());

                addListener(carbonManager, connectionItem.getAccount());
            }

        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                | SmackException.NotConnectedException | InterruptedException e) {
            LogManager.exception(this, e);
        }
    }

    // we need to remove old listener not to cause memory leak
    private void addListener(org.jivesoftware.smackx.carbons.CarbonManager carbonManager, AccountJid account) {
        CarbonCopyListener carbonCopyListener = carbonCopyListeners.remove(account);
        if (carbonCopyListener != null) {
            carbonManager.removeCarbonCopyReceivedListener(carbonCopyListener);
        }

        carbonCopyListener = new CarbonCopyListener(account);
        carbonCopyListeners.put(account, carbonCopyListener);
        carbonManager.addCarbonCopyReceivedListener(carbonCopyListener);
    }

    private boolean isCarbonsEnabledForConnection(ConnectionItem connection) {
        return org.jivesoftware.smackx.carbons.CarbonManager
                .getInstanceFor(connection.getConnection())
                .getCarbonsEnabled();
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