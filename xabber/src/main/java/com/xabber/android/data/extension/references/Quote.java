package com.xabber.android.data.extension.references;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class Quote extends ReferenceElement {
    private final int del;

    public Quote(int begin, int end, int del) {
        super(begin, end);
        this.del = del;
    }

    @Override
    public Type getType() {
        return Type.quote;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute(ATTRIBUTE_TYPE, getType());
        xml.attribute(ATTRIBUTE_BEGIN, begin);
        xml.attribute(ATTRIBUTE_END, end);
        xml.attribute(ATTRIBUTE_DEL, del);
        xml.rightAngleBracket();
        xml.closeElement(this);
        return xml;
    }

    public int getDel() {
        return del;
    }
}
