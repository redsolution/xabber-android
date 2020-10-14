package com.xabber.android.data.extension.xtoken;

import org.jivesoftware.smack.packet.IQ;

import java.util.List;

public class SessionsIQ extends IQ {

    public static final String ELEMENT   = "x";
    private static final String HASH_BLOCK = "#items";
    public static final String NAMESPACE = XTokenManager.NAMESPACE + HASH_BLOCK;

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
