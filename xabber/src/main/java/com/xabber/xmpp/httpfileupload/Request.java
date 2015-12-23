package com.xabber.xmpp.httpfileupload;

import com.xabber.xmpp.IQ;
import com.xabber.xmpp.SerializerUtils;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

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
        SerializerUtils.addTextTag(serializer, FILENAME, filename);
        SerializerUtils.addTextTag(serializer, SIZE, size);
        if (contentType != null) {
            SerializerUtils.addTextTag(serializer, CONTENT_TYPE, contentType);
        }
    }

    @Override
    public boolean isValid() {
        return filename != null && size != null;
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
