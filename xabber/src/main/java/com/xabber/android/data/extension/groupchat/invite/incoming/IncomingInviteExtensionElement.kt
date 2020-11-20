package com.xabber.android.data.extension.groupchat.invite.incoming

import com.xabber.android.data.message.chat.groupchat.GroupchatManager
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.NamedElement
import org.jivesoftware.smack.util.XmlStringBuilder

class IncomingInviteExtensionElement: ExtensionElement {

    var jid: String? = ""

    var reasonElement: ReasonElement? = null

    override fun toXML(): CharSequence = XmlStringBuilder().apply {
        attribute(JID_ATTRIBUTE, jid)
        rightAngleBracket()
        append(reasonElement?.toXML().toString())
        closeElement(INVITE_ELEMENT)
    }

    override fun getElementName() = INVITE_ELEMENT

    override fun getNamespace() = NAMESPACE

    companion object{
        const val HASH_BLOCK = "#invite"
        const val NAMESPACE = GroupchatManager.NAMESPACE + HASH_BLOCK
        const val INVITE_ELEMENT = "invite"
        const val JID_ATTRIBUTE = "jid"
    }

    class ReasonElement(val value: String): NamedElement{

        override fun toXML() = XmlStringBuilder(this).apply {
            rightAngleBracket()
            append(value)
            closeElement(ELEMENT_NAME)
        }

        override fun getElementName() = ELEMENT_NAME

        companion object {
            const val ELEMENT_NAME = "reason"
        }
    }

}