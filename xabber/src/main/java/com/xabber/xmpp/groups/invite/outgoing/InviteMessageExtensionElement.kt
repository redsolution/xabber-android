package com.xabber.xmpp.groups.invite.outgoing

import com.xabber.android.data.entity.ContactJid
import com.xabber.xmpp.groups.GroupExtensionElement
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.util.XmlStringBuilder

class InviteMessageExtensionElement(
    private val groupJid: ContactJid, private val reason: String?
) : ExtensionElement {

    override fun getNamespace() = NAMESPACE

    override fun getElementName() = ELEMENT

    override fun toXML() = XmlStringBuilder(this).apply {
        attribute(ATTRIBUTE_JID, groupJid.bareJid.toString())
        rightAngleBracket()
        if (!reason.isNullOrEmpty()) {
            optElement(ELEMENT_REASON, reason)
        }
        closeElement(ELEMENT)
    }

    companion object {
        const val ELEMENT = "invite"
        const val NAMESPACE =
            GroupExtensionElement.NAMESPACE + GroupchatInviteListQueryIQ.HASH_INVITE
        const val ATTRIBUTE_JID = "jid"
        const val ELEMENT_REASON = "reason"
    }

}