package com.xabber.android.data.extension.groupchat.settings;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class GroupchatIndexTypeNamedElement implements NamedElement {
    public static final String ELEMENT = "index";

    private String indexType;

    @Override
    public String getElementName() { return ELEMENT; }

    public GroupchatIndexTypeNamedElement(String indexType){
        this.indexType = indexType;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xmlStringBuilder = new XmlStringBuilder(this);
        xmlStringBuilder.rightAngleBracket();
        xmlStringBuilder.append(indexType);
        xmlStringBuilder.closeElement(this);
        return xmlStringBuilder;
    }

}
