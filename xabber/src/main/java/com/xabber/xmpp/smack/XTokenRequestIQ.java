package com.xabber.xmpp.smack;

import org.jivesoftware.smack.packet.IQ;

public class XTokenRequestIQ extends IQ {

    public static final String ELEMENT   = "issue";
    public static final String NAMESPACE = "http://xabber.com/protocol/auth-tokens";

    private final String client;
    private final String device;

    public XTokenRequestIQ(String client, String device) {
        super(ELEMENT, NAMESPACE);
        this.client = client;
        this.device = device;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.optElement("client", client);
        xml.optElement("device", device);
        return xml;
    }

    public String getClient() {
        return client;
    }

    public String getDevice() {
        return device;
    }
}
