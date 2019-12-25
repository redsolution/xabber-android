package com.xabber.xmpp.uuu;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;

public class ChatStateExtensionProvider extends ExtensionElementProvider<ChatStateExtension> {

    @Override
    public ChatStateExtension parse(XmlPullParser parser, int initialDepth) throws Exception {
        String chatStateString = parser.getName();
        ChatState state = ChatState.valueOf(chatStateString);
        ChatStateSubtype type = null;

        while (true) {
            int eventType = parser.nextTag();

            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("subtype")) {
                    String typeString = parser.getAttributeValue("", "type");
                    type = ChatStateSubtype.valueOf(typeString);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getDepth() == initialDepth) {
                    break;
                }
            }
        }
        return new ChatStateExtension(state, type);
    }
}
