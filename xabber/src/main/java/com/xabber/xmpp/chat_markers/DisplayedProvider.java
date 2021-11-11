package com.xabber.xmpp.chat_markers;

import com.xabber.android.data.extension.chat_markers.ChatMarkersElements;
import com.xabber.xmpp.sid.StanzaIdElement;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;

public class DisplayedProvider extends ExtensionElementProvider<ChatMarkersElements.DisplayedExtension> {

    @Override
    public ChatMarkersElements.DisplayedExtension parse(XmlPullParser parser, int initialDepth) throws Exception {
        String name;
        String id = parser.getAttributeValue("", "id");
        ChatMarkersElements.DisplayedExtension extension = new ChatMarkersElements.DisplayedExtension(id);
        outerloop:while (true) {
            int eventType = parser.next();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    if (name.equals(StanzaIdElement.ELEMENT)) {
                        String stanzaId = parser.getAttributeValue("", StanzaIdElement.ATTRIBUTE_ID);
                        String by = parser.getAttributeValue("", StanzaIdElement.ATTRIBUTE_BY);
                        if (stanzaId != null && by != null)
                            extension.addStanzaIdExtension(new StanzaIdElement(by, stanzaId));
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                    break;
                default:
                    break;
            }
        }
        return extension;
    }
}
