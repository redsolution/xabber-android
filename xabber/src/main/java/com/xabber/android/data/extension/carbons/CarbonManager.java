package com.xabber.android.data.extension.carbons;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.SettingsManager.SecurityOtrMode;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnAuthorizedListener;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;

import java.util.Collection;


/**
 * Packet extension for XEP-0280: Message Carbons. This class implements
 * the manager for registering {@link Carbon} support, enabling and disabling
 * message carbons.
 * <p/>
 * You should call enableCarbons() before sending your first undirected
 * presence.
 *
 * @author Georg Lukas, Semyon Baranov
 */
public class CarbonManager implements OnAuthorizedListener, OnPacketListener {
    private final static CarbonManager instance;

    static {
        instance = new CarbonManager();
        Application.getInstance().addManager(instance);
    }

    private CarbonManager() {
    }

    public static CarbonManager getInstance() {
        return instance;
    }


    @Override
    public void onAuthorized(final ConnectionItem connection) {
        if (!(connection instanceof AccountItem)) {
            return;
        }

        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                updateIsSupported(connection);
            }
        });
    }

    private void updateIsSupported(ConnectionItem connectionItem) {
        org.jivesoftware.smackx.carbons.CarbonManager carbonManager
                = org.jivesoftware.smackx.carbons.CarbonManager
                .getInstanceFor(connectionItem.getConnection());

        try {
            if (carbonManager.isSupportedByServer()) {
                carbonManager.setCarbonsEnabled(SettingsManager.connectionUseCarbons());
            }

        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                | SmackException.NotConnectedException | InterruptedException e) {
            LogManager.exception(this, e);
        }
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {

        if (!(packet instanceof Message)) {
            return;
        }

        if (!SettingsManager.connectionUseCarbons()) {
            return;
        }

        if (!isCarbonsEnabledForConnection(connection)) {
            return;
        }

        if (packet.getFrom() == null) {
            return;
        }


        final Message message = (Message) packet;
        CarbonExtension carbonExtension = CarbonExtension.from(message);

        if (carbonExtension == null) {
            return;
        }

        if (carbonExtension.getForwarded() == null) {
            return;
        }
        Message forwardedMsg = (Message) carbonExtension.getForwarded().getForwardedStanza();
        MessageManager.getInstance().displayForwardedMessage(connection, forwardedMsg, carbonExtension.getDirection());

    }

    public boolean isCarbonsEnabledForConnection(ConnectionItem connection) {
        return org.jivesoftware.smackx.carbons.CarbonManager
                .getInstanceFor(connection.getConnection())
                .getCarbonsEnabled();
    }

    /**
     * Sends the new state of message carbons to the server
     * when this setting has been changed
     */
    public void onUseCarbonsSettingsChanged() {
        Application.getInstance().runInBackground(new Runnable() {
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

        if (securityLevel == SecurityLevel.plain) {
            return;
        }

        if (isCarbonsEnabledForConnection(AccountManager.getInstance().getAccount(abstractChat.getAccount()))) {
            return;
        }
        CarbonExtension.Private.addTo(message);
    }
}