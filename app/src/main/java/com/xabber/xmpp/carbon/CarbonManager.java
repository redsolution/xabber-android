package com.xabber.xmpp.carbon;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.SettingsManager.SecurityOtrMode;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.extension.capability.OnServerInfoReceivedListener;
import com.xabber.android.data.extension.capability.ServerInfoManager;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageItem;
import com.xabber.android.data.message.MessageManager;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.ServiceDiscoveryManager;

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
    public static final String NAMESPACE = "urn:xmpp:carbons:2";
    private final static CarbonManager instance;

    static {
        instance = new CarbonManager();
        Application.getInstance().addManager(instance);

        Connection.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(final Connection connection) {

                if (!(connection instanceof XMPPConnection)) {
                    return;
                }

                ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
                if (sdm != null) {
                    sdm.addFeature(NAMESPACE);
                    sdm.addFeature(Forwarded.NAMESPACE);
                }
                instance.connection = connection;
            }
        });
    }

    private Connection connection;
    private volatile boolean enabled_state = false;

    private CarbonManager() {
    }

    public static CarbonManager getInstance() {

        return instance;
    }

    /**
     * Mark a message as "private", so it will not be carbon-copied.
     *
     * @param msg Message object to mark private
     */
    public static void disableCarbons(Message msg) {

        msg.addExtension(new Private());
    }

    private IQ carbonsEnabledIQ(final boolean new_state) {

        if (!checkConnected()) {
            return null;
        }
        IQ setIQ = new IQ() {
            public String getChildElementXML() {
                return String.format("<%s xmlns='%s'/>", new_state ? "enable" : "disable", NAMESPACE);
            }
        };
        setIQ.setType(IQ.Type.SET);
        setIQ.setFrom(connection.getUser());
        return setIQ;
    }

    private boolean checkConnected() {

        if (connection == null) {
            LogManager.exception(this, new Exception("connection is null"));
            return false;
        }
        if (!connection.isConnected()) {
            LogManager.exception(this, new Exception("not connected"));
            return false;
        }
        return true;
    }

    /**
     * Returns true if XMPP Carbons are supported by the server.
     *
     * @return true if supported
     */
    public boolean isSupportedByServer() {

        if (!checkConnected()) {
            return false;
        }
        boolean isCarbonSupported = ServerInfoManager.getInstance().isProtocolSupported(connection.getUser(), NAMESPACE);
        return isCarbonSupported;
    }

    /**
     * Notify server to change the carbons state. This method returns
     * immediately and changes the variable when the reply arrives.
     * <p/>
     * You should first check for support using isSupportedByServer().
     *
     * @param new_state whether carbons should be enabled or disabled
     */
    public void sendCarbonsEnabled(final boolean new_state) {

        LogManager.d(this, "sendCarbonsEnabled " + String.valueOf(new_state));

        if (!checkConnected()) {
            return;
        }

        IQ setIQ = carbonsEnabledIQ(new_state);
        connection.addPacketListener(new PacketListener() {
            public void processPacket(Packet packet) {
                IQ result = (IQ) packet;
                if (result.getType() == IQ.Type.RESULT) {
                    enabled_state = new_state;
                }
                connection.removePacketListener(this);
            }
        }, new PacketIDFilter(setIQ.getPacketID()));

        connection.sendPacket(setIQ);
    }

    /**
     * Helper method to enable carbons.
     */
    public void enableCarbons() {

        sendCarbonsEnabled(true);
    }

    /**
     * Helper method to disable carbons.
     */
    public void disableCarbons() {

        sendCarbonsEnabled(false);
    }

    /**
     * Check if carbons are enabled on this connection.
     */
    public boolean getCarbonsEnabled() {

        return enabled_state;
    }

    @Override
    public void onPacket(ConnectionItem connection, String bareAddress,
                         Packet packet) {

        if (!(connection instanceof AccountItem)) {
            return;
        }
        if (!SettingsManager.connectionUseCarbons()) {
            return;
        }
        final String user = packet.getFrom();
        if (user == null)
            return;
        if (!(packet instanceof Message))
            return;
        final Message message = (Message) packet;
        if (!getCarbonsEnabled()) {
            return;
        }
        PacketExtension carbonExtension = null;
        Direction dir = null;
        for (PacketExtension packetExtension : message.getExtensions()) {
            if (packetExtension instanceof Received) {
                carbonExtension = packetExtension;
                dir = Direction.received;
            } else if (packetExtension instanceof Sent) {
                carbonExtension = packetExtension;
                dir = Direction.sent;
            } else {
                continue;
            }
        }
        if (carbonExtension == null) {
            return;
        }
        Forwarded forwarded = null;
        if (dir == Direction.sent) {
            Sent carbon = (Sent) carbonExtension;
            forwarded = carbon.getForwarded();
        } else {
            Received carbon = (Received) carbonExtension;
            forwarded = carbon.getForwarded();
        }
        if (forwarded == null) {
            return;
        }
        Message forwardedMsg = (Message) forwarded.getForwardedPacket();
        MessageManager.getInstance().displayForwardedMessage(connection, forwardedMsg, dir);

    }

    @Override
    public void onServerInfoReceived(ConnectionItem connection) {

        if (isSupportedByServer()) {
            onUseCarbonsSettingsChanged();
        }
    }

    /**
     * Sends the new state of message carbons to the server
     * when this setting has been changed
     */
    public void onUseCarbonsSettingsChanged() {

        sendCarbonsEnabled(SettingsManager.connectionUseCarbons());
    }

    /**
     * Update outgoing message before sending.
     * Marks the message as non-carbon-copied in the following cases:
     * - Message Carbons is enabled and OTR mode is enabled.
     * - Message Carbons is enabled and OTR security level != plain.
     *
     * @param abstractChat
     * @param message      the <tt>Message</tt> to be sent
     * @param messageItem
     */
    public void updateOutgoingMessage(AbstractChat abstractChat, Message message, MessageItem messageItem) {

        if (!SettingsManager.connectionUseCarbons()) {
            return;
        }
        if (SettingsManager.securityOtrMode() == SecurityOtrMode.disabled) {
            return;
        }
        if (OTRManager.getInstance().getSecurityLevel(abstractChat.getAccount(),
                abstractChat.getUser()) != SecurityLevel.plain) {
            return;
        }
        message.addExtension(new Private());
    }

    public enum Direction {
        sent, received
    }
}