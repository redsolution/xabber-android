package com.xabber.android.data.extension.groupchat.create

import com.xabber.android.data.message.chat.groupchat.GroupChat
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.util.XmlStringBuilder

class CreatePtpGroupIQ(private val groupchat: GroupChat, private val memberId: String)
    : GroupchatCreateAbstractIQ() {

    init {
        to = groupchat.fullJidIfPossible ?: groupchat.contactJid.jid
        from = groupchat.account.bareJid
        type = Type.set
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(PtpElement(groupchat, memberId).toXML().toString())
    }

    class PtpElement(private val groupchat: GroupChat, private val memberId: String): ExtensionElement{

        override fun toXML(): CharSequence = XmlStringBuilder(this).apply {
            attribute(GROUP_JID_ATTRIBUTE, groupchat.contactJid.toString())
            attribute(MEMBER_ID_ATTRIBUTE, memberId)
            rightAngleBracket()
            closeElement(PEER_TO_PEER_ELEMENT)
        }

        override fun getNamespace() = null

        override fun getElementName() = PEER_TO_PEER_ELEMENT

        companion object{
            const val PEER_TO_PEER_ELEMENT = "peer-to-peer"
            const val GROUP_JID_ATTRIBUTE = "jid"
            const val MEMBER_ID_ATTRIBUTE = "id"
        }
    }
}