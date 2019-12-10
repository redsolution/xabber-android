package com.xabber.android.data.extension.rrr;

import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.log.LogManager;
import com.xabber.xmpp.smack.XMPPTCPConnection;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

public class RrrManager implements OnPacketListener {

    private static final String LOG_TAG = "RRRManager";
    public static final String NAMESPACE = "http://xabber.com/protocol/rewrite";

    private static RrrManager instance;

    public static RrrManager getInstance(){
        if (instance == null)
            instance = new RrrManager();
        return instance;
    }

    public boolean isSupported(XMPPTCPConnection connection)
    {
        try {
            return ServiceDiscoveryManager.getInstanceFor(connection).serverSupportsFeature(NAMESPACE);
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
            return false;
        }
    }

    public boolean isSupported(AccountItem accountItem) { return  isSupported(accountItem.getConnection()); }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {
        if (packet instanceof IQ){
            //TODO reaction to stanzas
        }
    }

}
