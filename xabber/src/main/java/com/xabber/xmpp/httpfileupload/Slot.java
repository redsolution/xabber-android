package com.xabber.xmpp.httpfileupload;


import org.jivesoftware.smack.packet.IQ;

/**
 * http://xmpp.org/extensions/xep-0363.html
 */
public class Slot extends IQ {

    public static final String ELEMENT_NAME = "slot";
    public static final String NAMESPACE = Request.NAMESPACE;

    public static final String PUT = "put";
    public static final String GET = "get";

    private String putUrl;
    private String getUrl;

    protected Slot() {
        super(ELEMENT_NAME, NAMESPACE);
        setType(Type.result);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        return null;
    }

    public void setPutUrl(String putUrl) {
        this.putUrl = putUrl;
    }

    public void setGetUrl(String getUrl) {
        this.getUrl = getUrl;
    }

    public String getPutUrl() {
        return putUrl;
    }

    public String getGetUrl() {
        return getUrl;
    }
}
