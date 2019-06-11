package com.xabber.android.data.extension.references;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public abstract class ReferenceElement implements ExtensionElement {

    public static final String NAMESPACE = "urn:xmpp:reference:0";
    public static final String ELEMENT = "reference";
    public static final String ELEMENT_BOLD = "bold";
    public static final String ELEMENT_ITALIC = "italic";
    public static final String ELEMENT_UNDERLINE = "underline";
    public static final String ELEMENT_STRIKE = "strike";
    public static final String ELEMENT_URL = "url";

    public static final String ATTRIBUTE_TYPE = "type";
    public static final String ATTRIBUTE_BEGIN = "begin";
    public static final String ATTRIBUTE_END = "end";
    public static final String ATTRIBUTE_URI = "uri";
    public static final String ATTRIBUTE_DEL = "del";

    public enum Type {
        data,
        forward,
        markup,
        mention,
        quote
    }

    protected final int begin;
    protected final int end;

    public ReferenceElement(int begin, int end) {
        this.begin = begin;
        this.end = end;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute(ATTRIBUTE_TYPE, getType());
        xml.attribute(ATTRIBUTE_BEGIN, begin);
        xml.attribute(ATTRIBUTE_END, end);
        xml.rightAngleBracket();
        appendToXML(xml);
        xml.closeElement(this);
        return xml;
    }

    public void appendToXML(XmlStringBuilder xml) { }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }

    public abstract Type getType();
}