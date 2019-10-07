package com.xabber.android.data.extension.xtoken;

import org.jivesoftware.smack.packet.IQ;

import java.util.List;

public class SessionsIQ extends IQ {

    public static final String ELEMENT   = "x";
    public static final String NAMESPACE = "http://xabber.com/protocol/auth-tokens#items";

    private final List<Session> sessions;

    public SessionsIQ(List<Session> sessions) {
        super(ELEMENT, NAMESPACE);
        this.sessions = sessions;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        return xml;
    }

    public List<Session> getSessions() {
        return sessions;
    }
}
