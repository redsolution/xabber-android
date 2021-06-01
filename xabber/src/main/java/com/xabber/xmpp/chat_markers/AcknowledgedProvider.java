package com.xabber.xmpp.chat_markers;

import com.xabber.android.data.extension.chat_markers.ChatMarkersElements;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;

public class AcknowledgedProvider extends ExtensionElementProvider<ChatMarkersElements.AcknowledgedExtension> {

    @Override
    public ChatMarkersElements.AcknowledgedExtension parse(XmlPullParser parser, int initialDepth) throws Exception {
        String id = parser.getAttributeValue("", "id");
        return new ChatMarkersElements.AcknowledgedExtension(id);
    }
}
