package com.xabber.android.data.extension.references.decoration;

import com.xabber.android.data.extension.references.ReferenceElement;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class Decoration extends ReferenceElement {

    public static final String MARKUP_NAMESPACE = "https://xabber.com/protocol/markup";
    public static final String ELEMENT_BOLD = "bold";
    public static final String ELEMENT_ITALIC = "italic";
    public static final String ELEMENT_UNDERLINE = "underline";
    public static final String ELEMENT_STRIKE = "strike";
    public static final String ELEMENT_LINK = "link";
    public static final String ELEMENT_QUOTE = "quote";

    public Decoration(int begin, int end) {
        super(begin, end);
    }

    @Override
    public Type getType() {
        return Type.decoration;
    }

    @Override
    public void appendToXML(XmlStringBuilder xml) { }
}
