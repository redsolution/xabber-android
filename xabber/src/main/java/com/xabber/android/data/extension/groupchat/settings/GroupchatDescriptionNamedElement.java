package com.xabber.android.data.extension.groupchat.settings;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class GroupchatDescriptionNamedElement implements NamedElement {
    public static final String ELEMENT = "description";

    private String description;

    @Override
    public String getElementName() { return ELEMENT; }

    public GroupchatDescriptionNamedElement(String description){
        this.description = description;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xmlStringBuilder = new XmlStringBuilder(this);
        xmlStringBuilder.rightAngleBracket();
        xmlStringBuilder.append(description);
        xmlStringBuilder.closeElement(this);
        return xmlStringBuilder;
    }
}
