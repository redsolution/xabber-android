package com.xabber.xmpp.sid;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.StandardExtensionElement;

/**
 * Created by valery.miller on 20.04.18.
 */

public class UniqStanzaHelper {

    private final static String ELEMENT_NAME = "stanza-id";
    private final static String NAMESPACE = "urn:xmpp:sid:0";
    private final static String ATTRIBUTE_ID = "id";

    public static String getStanzaId(Message message) {
        StandardExtensionElement sidElement = message.getExtension(ELEMENT_NAME, NAMESPACE);
        if (sidElement != null) return sidElement.getAttributeValue(ATTRIBUTE_ID);
        else return null;
    }

}
