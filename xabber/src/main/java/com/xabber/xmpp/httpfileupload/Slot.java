package com.xabber.xmpp.httpfileupload;


import com.xabber.xmpp.IQ;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

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
    public String getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public void serializeContent(XmlSerializer serializer) throws IOException {

    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();

        return xml;
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
