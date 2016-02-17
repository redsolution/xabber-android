package com.xabber.android.data.extension.carbons;

import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.SettingsManager.SecurityOtrMode;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.extension.capability.OnServerInfoReceivedListener;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;


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
public class CarbonManager implements OnServerInfoReceivedListener, OnPacketListener {
    private final static CarbonManager instance;

    static {
        instance = new CarbonManager();
        Application.getInstance().addManager(instance);

        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(final XMPPConnection connection) {

                if (connection == null) {
                    return;
                }

                instance.connection = connection;
            }
        });
    }

    private XMPPConnection connection;
    private volatile boolean enabled_state = false;

    private CarbonManager() {
    }

    public static CarbonManager getInstance() {

        return instance;
    }

    /**
     * Check if carbons are enabled on this connection.
     */
    public boolean getCarbonsEnabled() {

        return enabled_state;
    }

    @Override
    public void onPacket(ConnectionItem connection, String bareAddress, Stanza packet) {

        if (!(connection instanceof AccountItem)) {
            return;
        }
        if (!SettingsManager.connectionUseCarbons()) {
            return;
        }
        final String user = packet.getFrom();
        if (user == null) {
            return;
        }
        if (!(packet instanceof Message)) {
            return;
        }

        final Message message = (Message) packet;
        if (!getCarbonsEnabled()) {
            return;
        }
        CarbonExtension carbonExtension = CarbonExtension.from(message);

        if (carbonExtension == null) {
            return;
        }

        if (carbonExtension.getForwarded() == null) {
            return;
        }
        Message forwardedMsg = (Message) carbonExtension.getForwarded().getForwardedPacket();
        MessageManager.getInstance().displayForwardedMessage(connection, forwardedMsg, carbonExtension.getDirection());

    }

    @Override
    public void onServerInfoReceived(final ConnectionItem connection) {
        onUseCarbonsSettingsChanged();
    }

    /**
     * Sends the new state of message carbons to the server
     * when this setting has been changed
     */
    public void onUseCarbonsSettingsChanged() {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                org.jivesoftware.smackx.carbons.CarbonManager carbonManager
                        = org.jivesoftware.smackx.carbons.CarbonManager
                        .getInstanceFor(connection);
                try {
                    if (carbonManager.isSupportedByServer()) {
                        boolean useCarbons = SettingsManager.connectionUseCarbons();
                        carbonManager.setCarbonsEnabled(useCarbons);
                        enabled_state = useCarbons;
                    }
                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException e) {
                    e.printStackTrace();
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
        CarbonExtension.Private.addTo(message);
    }
}