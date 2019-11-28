package com.xabber.android.data.extension.reliablemessagedelivery;

import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.log.LogManager;
import com.xabber.xmpp.smack.XMPPTCPConnection;

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.jid.Jid;

public class ReliableMessageDeliveryManager implements OnPacketListener {

    public static final String NAMESPACE = "http://xabber.com/protocol/delivery";
    public static final String LOG_TAG = ReliableMessageDeliveryManager.class.getSimpleName();

    private static ReliableMessageDeliveryManager instance;

    public static ReliableMessageDeliveryManager getInstance(){
        if (instance == null)
            instance = new ReliableMessageDeliveryManager();
        return instance;
    }

    public static boolean isSupported(XMPPTCPConnection xmpptcpConnection){
        try {
            LogManager.i(LOG_TAG, ServiceDiscoveryManager.getInstanceFor(xmpptcpConnection).getFeatures().toString());
            return ServiceDiscoveryManager.getInstanceFor(xmpptcpConnection).serverSupportsFeature(NAMESPACE);
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
            return false;
        }
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {

    }
}
