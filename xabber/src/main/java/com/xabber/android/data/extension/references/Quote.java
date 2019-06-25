package com.xabber.android.data.extension.references;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class Quote extends ReferenceElement {
    private final String marker;

    public Quote(int begin, int end, String marker) {
        super(begin, end);
        this.marker = marker;
    }

    @Override
    public Type getType() {
        return Type.quote;
    }

    @Override
    public void appendToXML(XmlStringBuilder xml) {
        if (marker != null && !marker.isEmpty()) {
            xml.element(ELEMENT_MARKER, marker);
        }
    }

    public String getMarker() {
        return marker;
    }
}
