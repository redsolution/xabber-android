package com.xabber.xmpp.sid;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by valery.miller on 20.04.18.
 */

public class UniqueStanzaHelper {

    final static String NAMESPACE = "urn:xmpp:sid:0";

    public static String getOriginId(Message message) {
        OriginIdElement oidElement = message.getExtension(OriginIdElement.ELEMENT, OriginIdElement.NAMESPACE);
        if (oidElement != null) return oidElement.getId();
        else return message.getStanzaId();
    }

    public static String getStanzaIdBy(Message message, String by){
        List<ExtensionElement> stanzaIds = new ArrayList<>(message.getExtensions(StanzaIdElement.ELEMENT, StanzaIdElement.NAMESPACE));
        String messageId = "";
        if (stanzaIds.isEmpty()) return "";
        for (ExtensionElement stanzaIdElement : stanzaIds) {
            if (stanzaIdElement instanceof StanzaIdElement) {
                String idBy = ((StanzaIdElement) stanzaIdElement).getBy();
                if (idBy != null && idBy.equals(by)) {
                    messageId = ((StanzaIdElement) stanzaIdElement).getId();
                    break;
                } else {
                    messageId = ((StanzaIdElement) stanzaIdElement).getId();
                }
            }
        }

        return messageId;
    }

}
