package com.xabber.android.data.extension.references.data;

import com.xabber.android.data.extension.references.ReferenceElement;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class Data extends ReferenceElement {

    public Data(int begin, int end) {
        super(begin, end);
    }

    @Override
    public Type getType() {
        return Type.data;
    }

    @Override
    public void appendToXML(XmlStringBuilder xml) { }
}
