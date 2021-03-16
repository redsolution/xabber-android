package com.xabber.android.data.extension.groupchat

import com.xabber.android.data.groups.GroupsManager
import com.xabber.xmpp.avatar.MetadataInfo
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.util.XmlStringBuilder

class GroupMemberExtensionElement(val id: String, val nickname: String, val role: String) : ExtensionElement {
    var jid: String? = null
    var badge = ""
    var lastPresent: String? = null
    var subscription: String? = null
    var avatarInfo: MetadataInfo? = null

    override fun getNamespace() = NAMESPACE

    override fun getElementName() = ELEMENT

    override fun toXML() = XmlStringBuilder(this).apply {
        attribute(ATTR_ID, id)
        rightAngleBracket()

        addSimpleElement(ELEMENT_ROLE, role)
        addSimpleElement(ELEMENT_NICKNAME, nickname)
        addSimpleElement(ELEMENT_BADGE, badge)

        if (jid != null) {
            addSimpleElement(ELEMENT_JID, jid)
        }

        if (subscription != null) {
            addSimpleElement(ELEMENT_SUBSCRIPTION, subscription)
        }

        halfOpenElement(ELEMENT_METADATA)
        xmlnsAttribute(NAMESPACE_METADATA)
        rightAngleBracket()
        if (avatarInfo != null) {
            halfOpenElement(ELEMENT_INFO)
            attribute("id", avatarInfo?.id)
            attribute("bytes", avatarInfo?.bytes.toString())
            attribute("type", avatarInfo?.type)
            attribute("url", avatarInfo?.url.toString())
            if (avatarInfo!!.height > 0) attribute("height", avatarInfo!!.height.toInt())
            if (avatarInfo!!.width > 0) attribute("width", avatarInfo!!.width.toInt())
            closeEmptyElement()
        }
        closeElement(ELEMENT_METADATA)

        closeElement(ELEMENT)
    }

    private fun XmlStringBuilder.addSimpleElement(element: String, value: String?){
        openElement(element)
        append(value ?: "")
        closeElement(element)
    }

    companion object {
        const val ELEMENT = "user"
        const val NAMESPACE = GroupsManager.NAMESPACE
        const val ATTR_ID = "id"
        const val ELEMENT_JID = "jid"
        const val ELEMENT_NICKNAME = "nickname"
        const val ELEMENT_ROLE = "role"
        const val ELEMENT_BADGE = "badge"
        const val ELEMENT_SUBSCRIPTION = "subscription"
        const val ELEMENT_METADATA = "metadata"
        const val ELEMENT_PRESENT = "present"
        const val NAMESPACE_METADATA = "urn:xmpp:avatar:metadata"
        const val ELEMENT_INFO = "info"
    }

}