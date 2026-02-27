package com.xabber.android.data.extension.references.mutable;

import com.xabber.android.data.extension.references.ReferenceElement;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class Mutable extends ReferenceElement {

    public Mutable(int begin, int end) {
        super(begin, end);
    }

    @Override
    public Type getType() {
        return Type.mutable;
    }

    @Override
    public void appendToXML(XmlStringBuilder xml) { }
}
