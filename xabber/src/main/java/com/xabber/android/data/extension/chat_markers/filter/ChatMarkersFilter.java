package com.xabber.android.data.extension.chat_markers.filter;

import com.xabber.android.data.extension.chat_markers.ChatMarkersElements;

import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;

public final class ChatMarkersFilter extends StanzaExtensionFilter {

    public static final StanzaFilter INSTANCE = new ChatMarkersFilter(ChatMarkersElements.NAMESPACE);

    private ChatMarkersFilter(String namespace) {
        super(namespace);
    }
}
