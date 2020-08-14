package com.xabber.android.data.extension.groupchat.settings;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class GroupchatMembershipTypeNamedElement implements NamedElement {
    public static final String ELEMENT = "membership";

    private String membershipType;

    @Override
    public String getElementName() { return ELEMENT; }

    public GroupchatMembershipTypeNamedElement(String membershipType){
        this.membershipType = membershipType;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xmlStringBuilder = new XmlStringBuilder(this);
        xmlStringBuilder.rightAngleBracket();
        xmlStringBuilder.append(membershipType);
        xmlStringBuilder.closeElement(this);
        return xmlStringBuilder;
    }
}
