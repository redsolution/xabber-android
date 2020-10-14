package com.xabber.android.data.extension.xtoken;

import org.jivesoftware.smack.packet.IQ;

public class SessionsRequestIQ extends IQ {

    public static final String ELEMENT   = "query";
    private static final String HASH_BLOCK = "#items";
    public static final String NAMESPACE = XTokenManager.NAMESPACE + HASH_BLOCK;

    public SessionsRequestIQ() {
        super(ELEMENT, NAMESPACE);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        return xml;
    }

}
