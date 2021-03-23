package com.xabber.xmpp.sid;

import com.xabber.xmpp.archive.ArchivedIdElement;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by valery.miller on 20.04.18.
 */

public class UniqueIdsHelper {

    public static String getOriginId(Message message) {
        OriginIdElement oidElement = message.getExtension(OriginIdElement.ELEMENT, OriginIdElement.NAMESPACE);
        if (oidElement != null) return oidElement.getId();
        else return null;
    }

    public static String getStanzaIdBy(Message message, String by){
        List<ExtensionElement> stanzaIds =
                new ArrayList<>(message.getExtensions(StanzaIdElement.ELEMENT, StanzaIdElement.NAMESPACE));

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

    public static String getArchivedIdBy(Stanza stanza, String by) {
        List<ExtensionElement> archivedIdElements =
                new ArrayList<>(stanza.getExtensions(ArchivedIdElement.ELEMENT, ArchivedIdElement.NAMESPACE));
        String messageId = "";

        if (archivedIdElements.isEmpty()) return "";

        for (ExtensionElement archivedIdElement : archivedIdElements) {
            if (archivedIdElement instanceof ArchivedIdElement) {
                String idBy = ((ArchivedIdElement) archivedIdElement).getBy();
                if (idBy.equals(by)) {
                    messageId = ((ArchivedIdElement) archivedIdElement).getId();
                    break;
                } else {
                    messageId = ((ArchivedIdElement) archivedIdElement).getId();
                }
            }
        }

        return messageId;
    }

}
