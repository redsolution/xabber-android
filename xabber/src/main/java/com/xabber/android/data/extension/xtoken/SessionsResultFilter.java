package com.xabber.android.data.extension.xtoken;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.IQResultReplyFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;

public class SessionsResultFilter extends IQResultReplyFilter {

    public SessionsResultFilter(IQ iqPacket, XMPPConnection conn) {
        super(iqPacket, conn);
    }

    @Override
    public boolean accept(Stanza packet) {
        if (!super.accept(packet)) {
            return false;
        }
        return SessionsIQ.ELEMENT.equals(((IQ)packet).getChildElementName()) &&
                SessionsIQ.NAMESPACE.equals(((IQ)packet).getChildElementNamespace());
    }

}
