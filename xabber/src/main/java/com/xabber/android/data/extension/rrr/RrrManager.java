package com.xabber.android.data.extension.rrr;

import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.xmpp.smack.XMPPTCPConnection;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;

public class RrrManager implements OnPacketListener {

    private static RrrManager instance;

    public static RrrManager getInstance(){
        if (instance == null)
            instance = new RrrManager();
        return instance;
    }

    public boolean isSupported(XMPPTCPConnection connection){ return true;} //TODO change this

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {
        if (packet instanceof IQ){
            //TODO reaction to stanzas
        }
    }
}
