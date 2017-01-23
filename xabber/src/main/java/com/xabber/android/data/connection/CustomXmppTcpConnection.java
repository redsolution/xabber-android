package com.xabber.android.data.connection;

import com.xabber.android.data.log.LogManager;

import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.dns.HostAddress;

import java.util.LinkedList;
import java.util.List;

class CustomXmppTcpConnection extends XMPPTCPConnection {
    private static final String LOG_TAG = CustomXmppTcpConnection.class.getSimpleName();

    CustomXmppTcpConnection(XMPPTCPConnectionConfiguration config) {
        super(config);
        LogManager.i(LOG_TAG, "New CustomXmppTcpConnection");
    }

    @Override
    protected List<HostAddress> populateHostAddresses() {
        if (hostAddresses != null && !hostAddresses.isEmpty()) {
            LogManager.i(LOG_TAG, "populateHostAddresses host addresses already populated - returning");
            return new LinkedList<>();
        }

        LogManager.i(LOG_TAG, "populateHostAddresses");
        return super.populateHostAddresses();
    }
}
