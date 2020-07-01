package com.xabber.xmpp.sid;

import com.xabber.android.data.extension.reliablemessagedelivery.OriginIdElement;
import com.xabber.android.data.extension.reliablemessagedelivery.StanzaIdElement;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by valery.miller on 20.04.18.
 */

public class UniqStanzaHelper {

    final static String ELEMENT_NAME_ORIGIN = "origin-id";
    final static String ELEMENT_NAME = "stanza-id";
    final static String NAMESPACE = "urn:xmpp:sid:0";
    final static String ATTRIBUTE_ID = "id";

    public static String getStanzaId(Message message) {
        StanzaIdElement sidElement = message.getExtension(ELEMENT_NAME, NAMESPACE);
        if (sidElement != null) return sidElement.getId();
        else return null;
    }

    public static String getOriginId(Message message) {
        OriginIdElement oidElement = message.getExtension(ELEMENT_NAME_ORIGIN, NAMESPACE);
        if (oidElement != null) return oidElement.getId();
        else return message.getStanzaId();
    }

    public static String getContactStanzaId(Message message){
        List<ExtensionElement> stanzaIds = new ArrayList<>(message.getExtensions(StanzaIdElement.ELEMENT, StanzaIdElement.NAMESPACE));
        String messageId = "";
        if (stanzaIds.isEmpty()) return "";
        for (ExtensionElement stanzaIdElement : stanzaIds) {
            if (stanzaIdElement instanceof StanzaIdElement) {
                String idBy = ((StanzaIdElement) stanzaIdElement).getBy();
                if (idBy != null && idBy.equals(message.getFrom())) {
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
