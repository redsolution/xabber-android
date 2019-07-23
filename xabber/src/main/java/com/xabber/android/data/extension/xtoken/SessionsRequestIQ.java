package com.xabber.android.data.extension.xtoken;

import org.jivesoftware.smack.packet.IQ;

public class SessionsRequestIQ extends IQ {

    public static final String ELEMENT   = "query";
    public static final String NAMESPACE = "http://xabber.com/protocol/auth-tokens#items";

    public SessionsRequestIQ() {
        super(ELEMENT, NAMESPACE);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        return xml;
    }

}
