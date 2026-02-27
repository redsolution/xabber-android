package com.xabber.android.data.extension.archive

import com.xabber.xmpp.mam.MamResultExtensionElement
import org.jivesoftware.smack.filter.StanzaFilter
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza

class MamResultsStanzaFilter : StanzaFilter {
    override fun accept(stanza: Stanza?): Boolean {
        return stanza is Message && stanza.hasExtension(
            MamResultExtensionElement.ELEMENT, MessageArchiveManager.NAMESPACE
        )
    }
}