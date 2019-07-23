package com.xabber.android.data.extension.mam;

import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.Stanza;

public class ArchivedHelper {

    private final static String ELEMENT_NAME = "archived";
    private final static String NAMESPACE = "urn:xmpp:mam:tmp";
    private final static String ATTRIBUTE_ID = "id";
    private final static String ATTRIBUTE_BY = "by";

    public static String getArchivedId(Stanza stanza) {
        StandardExtensionElement sidElement = stanza.getExtension(ELEMENT_NAME, NAMESPACE);
        if (sidElement != null) return sidElement.getAttributeValue(ATTRIBUTE_ID);
        else return null;
    }

    public static String getArchivedBy(Stanza stanza) {
        StandardExtensionElement sidElement = stanza.getExtension(ELEMENT_NAME, NAMESPACE);
        if (sidElement != null) return sidElement.getAttributeValue(ATTRIBUTE_BY);
        else return null;
    }

}
