package com.xabber.xmpp.chat_state;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class ChatStateExtension implements ExtensionElement {

    public static final String NAMESPACE = "http://jabber.org/protocol/chatstates";
    public static final String UUU_NAME = "subtype";
    public static final String UUU_NAMESPACE = "https://xabber.com/protocol/extended-chatstates";

    private final ChatState state;
    private final ChatStateSubtype type;

    /**
     * Default constructor. The argument provided is the state that the extension will represent.
     *
     * @param state the state that the extension represents.
     */
    public ChatStateExtension(ChatState state) {
        this.state = state;
        this.type = null;
    }

    public ChatStateExtension(ChatState state, ChatStateSubtype type) {
        this.state = state;
        this.type = type;
    }

    @Override
    public String getElementName() {
        return state.name();
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public ChatState getChatState() {
        return state;
    }

    public ChatStateSubtype getType() {
        return type;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        if (type != null) {
            xml.rightAngleBracket();
            xml.prelude(UUU_NAME, UUU_NAMESPACE);
            xml.attribute("type", type.name());
            xml.closeEmptyElement();
            xml.closeElement(state.name());
        } else
            xml.closeEmptyElement();
        return xml;
    }

}
