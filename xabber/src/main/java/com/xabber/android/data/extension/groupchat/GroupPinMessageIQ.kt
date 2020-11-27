package com.xabber.android.data.extension.groupchat

import com.xabber.android.data.message.chat.groupchat.GroupchatManager
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.NamedElement
import org.jivesoftware.smack.util.XmlStringBuilder
import org.jxmpp.jid.Jid

class GroupPinMessageIQ(from: Jid, to: Jid, val messageId: String): IQ(UPDATE_ELEMENT_NAME, NAMESPACE) {

    init {
        this.type = Type.set
        this.from = from
        this.to = to.asEntityFullJidIfPossible() ?: to
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(GroupPinMessageElement(messageId).toXML().toString())
    }

    private companion object {
        const val NAMESPACE = GroupchatManager.NAMESPACE
        const val UPDATE_ELEMENT_NAME = "update"
    }

    private class GroupPinMessageElement(private val messageStanzaId: String): NamedElement {

        override fun getElementName() = PINNED_ELEMENT_NAME

        override fun toXML() = XmlStringBuilder(this).apply {
            rightAngleBracket()
            append(messageStanzaId)
            closeElement(PINNED_ELEMENT_NAME)
        }

        private companion object {
            const val PINNED_ELEMENT_NAME = "pinned"
        }

    }

}