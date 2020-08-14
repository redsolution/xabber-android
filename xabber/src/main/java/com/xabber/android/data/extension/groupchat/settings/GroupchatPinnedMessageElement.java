package com.xabber.android.data.extension.groupchat.settings;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class GroupchatPinnedMessageElement implements NamedElement {

    public static final String ELEMENT = "pinned-message";

    private String messageStanzaId;

    @Override
    public String getElementName() { return ELEMENT; }

    public GroupchatPinnedMessageElement(String messageStanzaId){
        this.messageStanzaId = messageStanzaId;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xmlStringBuilder = new XmlStringBuilder(this);
        xmlStringBuilder.rightAngleBracket();
        xmlStringBuilder.append(messageStanzaId);
        xmlStringBuilder.closeElement(this);
        return xmlStringBuilder;
    }
}
