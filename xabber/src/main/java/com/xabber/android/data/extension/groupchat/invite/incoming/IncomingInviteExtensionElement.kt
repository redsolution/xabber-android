package com.xabber.android.data.extension.groupchat.invite.incoming

import com.xabber.android.data.message.chat.groupchat.GroupchatManager
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.NamedElement
import org.jivesoftware.smack.util.XmlStringBuilder

class IncomingInviteExtensionElement: ExtensionElement {

    var groupJid: String? = ""

    private var reasonElement: ReasonElement? = null

    fun setReason(reason: String){
        reasonElement = ReasonElement(reason)
    }

    fun getReason() = reasonElement?.reason

    override fun toXML(): CharSequence = XmlStringBuilder().apply {
        attribute(JID_ATTRIBUTE, groupJid)
        rightAngleBracket()
        append(reasonElement?.toXML().toString())
        closeElement(ELEMENT)
    }

    override fun getElementName() = ELEMENT

    override fun getNamespace() = NAMESPACE

    companion object{
        const val HASH_BLOCK = "#invite"
        const val NAMESPACE = GroupchatManager.NAMESPACE + HASH_BLOCK
        const val ELEMENT = "invite"
        const val JID_ATTRIBUTE = "jid"
    }

    class ReasonElement(val reason: String): NamedElement{

        override fun toXML() = XmlStringBuilder(this).apply {
            rightAngleBracket()
            append(reason)
            closeElement(ELEMENT_NAME)
        }

        override fun getElementName() = ELEMENT_NAME

        companion object {
            const val ELEMENT_NAME = "reason"
        }
    }

}