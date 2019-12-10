package com.xabber.xmpp.chat_markers;

import com.xabber.android.data.extension.chat_markers.ChatMarkersElements;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;

public class MarkableProvider extends ExtensionElementProvider<ChatMarkersElements.MarkableExtension> {

    @Override
    public ChatMarkersElements.MarkableExtension parse(XmlPullParser parser, int initialDepth) throws Exception {
        return new ChatMarkersElements.MarkableExtension();
    }
}
