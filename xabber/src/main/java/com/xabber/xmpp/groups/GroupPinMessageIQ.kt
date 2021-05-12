package com.xabber.xmpp.groups

import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.extension.groups.GroupsManager
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.NamedElement
import org.jivesoftware.smack.util.XmlStringBuilder
import org.jxmpp.jid.Jid

class GroupPinMessageIQ(to: Jid, val messageId: String) : IQ(UPDATE_ELEMENT_NAME, NAMESPACE) {

    init {
        this.type = Type.set
        this.to = to
    }

    constructor(groupChat: GroupChat, messageId: String)
            : this(groupChat.fullJidIfPossible ?: groupChat.contactJid.jid, messageId)

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(GroupPinMessageElement(messageId).toXML().toString())
    }

    private companion object {
        const val NAMESPACE = GroupsManager.NAMESPACE
        const val UPDATE_ELEMENT_NAME = "update"
    }

    private class GroupPinMessageElement(private val messageStanzaId: String) : NamedElement {

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