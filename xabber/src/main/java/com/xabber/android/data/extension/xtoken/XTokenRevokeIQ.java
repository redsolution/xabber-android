package com.xabber.android.data.extension.xtoken;

import org.jivesoftware.smack.packet.IQ;

import java.util.List;

public class XTokenRevokeIQ extends IQ {

    public static final String ELEMENT           = "revoke";
    public static final String ELEMENT_TOKEN_UID = "token-uid";
    public static final String NAMESPACE         = "http://xabber.com/protocol/auth-tokens";

    private List<String> ids;

    public XTokenRevokeIQ(List<String> ids) {
        super(ELEMENT, NAMESPACE);
        this.ids = ids;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        for (String id : ids) {
            xml.optElement(ELEMENT_TOKEN_UID, id);
        }
        return xml;
    }
}
