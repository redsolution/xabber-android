package com.xabber.android.data.extension.xtoken;

import org.jivesoftware.smack.packet.IQ;

public class XTokenIQ extends IQ {

    public static final String NAMESPACE         = "http://xabber.com/protocol/auth-tokens";
    public static final String ELEMENT           = "x";
    public static final String ELEMENT_TOKEN     = "token";
    public static final String ELEMENT_TOKEN_UID = "token-uid";
    public static final String ELEMENT_EXPIRE    = "expire";

    private final String token;
    private final String uid;
    private final long expire;

    public XTokenIQ(String token, String uid, long expire) {
        super(ELEMENT, NAMESPACE);
        this.token = token;
        this.uid = uid;
        this.expire = expire;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.element(ELEMENT_TOKEN, token);
        xml.element(ELEMENT_TOKEN_UID, uid);
        xml.element(ELEMENT_EXPIRE, String.valueOf(expire));
        return xml;
    }

    public String getToken() {
        return token;
    }

    public String getUid() {
        return uid;
    }

    public long getExpire() {
        return expire;
    }
}
