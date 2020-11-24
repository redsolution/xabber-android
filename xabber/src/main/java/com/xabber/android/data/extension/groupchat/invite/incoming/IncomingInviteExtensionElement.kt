package com.xabber.android.data.extension.groupchat.invite.incoming

import com.xabber.android.data.message.chat.groupchat.GroupchatManager
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.NamedElement
import org.jivesoftware.smack.util.XmlStringBuilder

class IncomingInviteExtensionElement: ExtensionElement {

    var groupJid: String = ""

    private var reasonElement: ReasonElement? = null
    private var userElement: UserElement? = null

    fun setReason(reason: String){ reasonElement = ReasonElement(reason) }
    fun getReason() = reasonElement?.reason ?: ""

    fun setUser(jid: String = "", id: String = ""){ userElement = UserElement(jid, id) }

    fun getUserJid() = userElement?.jid ?: ""
    fun getUserId() = userElement?.id ?: ""

    override fun toXML(): CharSequence = XmlStringBuilder().apply {
        halfOpenElement(ELEMENT)
        attribute(JID_ATTRIBUTE, groupJid)
        xmllangAttribute(NAMESPACE)
        rightAngleBracket()
        if (reasonElement != null) append(reasonElement?.toXML().toString())
        if (userElement != null) append(userElement?.toXML().toString())
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

    class UserElement(val jid: String = "", val id: String = ""): ExtensionElement {

        override fun toXML() = XmlStringBuilder().apply {
            halfOpenElement(ELEMENT)
            attribute(JID_ATTRIBUTE, jid)
            attribute(ID_ATTRIBUTE, id)
            closeEmptyElement()
        }

        override fun getElementName() = ELEMENT

        override fun getNamespace() = null

        companion object{
            const val ELEMENT = "user"
            const val JID_ATTRIBUTE = "jid"
            const val ID_ATTRIBUTE = "id"
        }

    }

}