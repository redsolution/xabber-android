package com.xabber.android.data.extension.references;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class Mention extends ReferenceElement {
    private final String uri; // jid

    public Mention(int begin, int end, String uri) {
        super(begin, end);
        this.uri = uri;
    }

    @Override
    public Type getType() {
        return Type.mention;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute(ATTRIBUTE_TYPE, getType());
        xml.attribute(ATTRIBUTE_BEGIN, begin);
        xml.attribute(ATTRIBUTE_END, end);
        xml.attribute(ATTRIBUTE_URI, uri);
        xml.rightAngleBracket();
        xml.closeElement(this);
        return xml;
    }

    public String getUri() {
        return uri;
    }
}
