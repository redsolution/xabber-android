package com.xabber.xmpp.chat_markers;

import com.xabber.android.data.extension.chat_markers.ChatMarkersElements;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;

public class ReceivedProvider extends ExtensionElementProvider<ChatMarkersElements.ReceivedExtension> {

    @Override
    public ChatMarkersElements.ReceivedExtension parse(XmlPullParser parser, int initialDepth) throws Exception {
        final String ELEMENT_ID_NAME = "stanza-id";
        String name;
        String id = parser.getAttributeValue("", "id");
        ChatMarkersElements.ReceivedExtension extension = new ChatMarkersElements.ReceivedExtension(id);
        outerloop:while (true) {
            int eventType = parser.next();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    if (name.equals(ELEMENT_ID_NAME)) {
                        String stanzaId = parser.getAttributeValue("", "id");
                        if (stanzaId != null)
                            extension.setStanzaId(parser.getAttributeValue("", "id"));
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
