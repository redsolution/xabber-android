package com.xabber.android.data.extension.groupchat.settings;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class GroupchatGroupNameNamedElement implements NamedElement{
    public static final String ELEMENT = "name";

    private String groupName;

    @Override
    public String getElementName() { return ELEMENT; }

    public GroupchatGroupNameNamedElement(String groupName){
        this.groupName = groupName;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xmlStringBuilder = new XmlStringBuilder(this);
        xmlStringBuilder.rightAngleBracket();
        xmlStringBuilder.append(groupName);
        xmlStringBuilder.closeElement(this);
        return xmlStringBuilder;
    }
}
