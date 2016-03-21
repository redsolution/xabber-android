package com.xabber.xmpp.httpfileupload;

import org.jivesoftware.smack.packet.IQ;

/**
 * http://xmpp.org/extensions/xep-0363.html
 */
public class Request extends IQ {

    public static final String ELEMENT_NAME = "request";
    public static final String NAMESPACE = "urn:xmpp:http:upload";

    public static final String FILENAME = "filename";
    public static final String SIZE = "size";
    public static final String CONTENT_TYPE = "content-type";

    private String filename;
    private String size;
    private String contentType;

    public Request() {
        super(ELEMENT_NAME, NAMESPACE);
        setType(Type.get);
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.element(FILENAME, filename);
        xml.element(SIZE, size);
        if (contentType != null) {
            xml.element(CONTENT_TYPE, contentType);
        }
        return xml;
    }
}
