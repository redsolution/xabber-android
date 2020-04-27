package com.xabber.android.data.extension.vcard;

import com.xabber.xmpp.vcard.VCard;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.id.StanzaIdUtil;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.jid.Jid;

import java.util.Map;
import java.util.WeakHashMap;

public class CustomVCardManager extends Manager {

    public static final String NAMESPACE = VCard.NAMESPACE;
    public static final String ELEMENT = VCard.ELEMENT;

    private static final Map<XMPPConnection, CustomVCardManager> INSTANCES = new WeakHashMap<>();

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(CustomVCardManager::getInstanceFor);
    }

    /**
     * Retrieves a {@link VCardManager} for the specified {@link XMPPConnection}, creating one if it doesn't already
     * exist.
     *
     * @param connection the connection the manager is attached to.
     * @return The new or existing manager.
     */
    public static synchronized CustomVCardManager getInstanceFor(XMPPConnection connection) {
        CustomVCardManager vcardManager = INSTANCES.get(connection);
        if (vcardManager == null) {
            vcardManager = new CustomVCardManager(connection);
            INSTANCES.put(connection, vcardManager);
        }
        return vcardManager;
    }

    private CustomVCardManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(NAMESPACE);
    }

    /**
     * Save this vCard for the user connected by 'connection'. XMPPConnection should be authenticated
     * and not anonymous.
     *
     * @throws XMPPException.XMPPErrorException thrown if there was an issue setting the VCard in the server.
     * @throws SmackException.NoResponseException if there was no response from the server.
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     */
    public void saveVCard(VCard vcard) throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException {
        // XEP-54 § 3.2 "A user may publish or update his or her vCard by sending an IQ of type "set" with no 'to' address…"
        vcard.setTo((Jid) null);
        vcard.setType(IQ.Type.set);
        // Also make sure to generate a new stanza id (the given vcard could be a vcard result), in which case we don't
        // want to use the same stanza id again (although it wouldn't break if we did)
        vcard.setStanzaId(StanzaIdUtil.newStanzaId());
        connection().createStanzaCollectorAndSend(vcard).nextResultOrThrow();
    }

    public void sendVCardRequest(Jid jid) throws SmackException.NotConnectedException, InterruptedException {
        VCard vcardRequest = new VCard();
        vcardRequest.setTo(jid);
        connection().sendStanza(vcardRequest);
    }

}
