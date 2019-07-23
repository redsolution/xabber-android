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
    public void appendToXML(XmlStringBuilder xml) {
        if (uri != null && uri.isEmpty()) {
            xml.element(ELEMENT_URI, uri);
        }
    }

    public String getUri() {
        return uri;
    }
}
