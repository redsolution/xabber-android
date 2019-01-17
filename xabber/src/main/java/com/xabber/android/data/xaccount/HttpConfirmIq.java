package com.xabber.android.data.xaccount;

import org.jivesoftware.smack.packet.IQ;

public class HttpConfirmIq extends IQ {

    public static final String NAMESPACE = "http://jabber.org/protocol/http-auth";
    public static final String ELEMENT = "confirm";
    public static final String PROP_ID = "id";
    public static final String PROP_URL = "url";
    public static final String PROP_METHOD = "method";

    private String id;
    private String method;
    protected String url;

    public HttpConfirmIq() {
        super(ELEMENT, NAMESPACE);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        return null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
