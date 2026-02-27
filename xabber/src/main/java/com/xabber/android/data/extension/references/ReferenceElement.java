package com.xabber.android.data.extension.references;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public abstract class ReferenceElement implements ExtensionElement {

    public static final String NAMESPACE = "https://xabber.com/protocol/references";
    public static final String ELEMENT = "reference";
    public static final String ATTRIBUTE_TYPE = "type";
    public static final String ATTRIBUTE_BEGIN = "begin";
    public static final String ATTRIBUTE_END = "end";

    public enum Type {
        decoration,
        mutable,
        data
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