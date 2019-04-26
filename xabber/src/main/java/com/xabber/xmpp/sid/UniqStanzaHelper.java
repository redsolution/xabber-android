package com.xabber.xmpp.sid;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.StandardExtensionElement;

/**
 * Created by valery.miller on 20.04.18.
 */

public class UniqStanzaHelper {

    final static String ELEMENT_NAME_ORIGIN = "origin-id";
    final static String ELEMENT_NAME = "stanza-id";
    final static String NAMESPACE = "urn:xmpp:sid:0";
    final static String ATTRIBUTE_ID = "id";

    public static String getStanzaId(Message message) {
        StandardExtensionElement sidElement = message.getExtension(ELEMENT_NAME, NAMESPACE);
        if (sidElement != null) return sidElement.getAttributeValue(ATTRIBUTE_ID);
        else return null;
    }

    public static String getOriginId(Message message) {
        StandardExtensionElement sidElement = message.getExtension(ELEMENT_NAME_ORIGIN, NAMESPACE);
        if (sidElement != null) return sidElement.getAttributeValue(ATTRIBUTE_ID);
        else return null;
    }

}
