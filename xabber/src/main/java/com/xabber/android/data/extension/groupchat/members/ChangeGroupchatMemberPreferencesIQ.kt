package com.xabber.android.data.extension.groupchat.members

import com.xabber.android.data.message.chat.groupchat.GroupChat
import com.xabber.android.data.message.chat.groupchat.GroupchatManager
import com.xabber.xmpp.SimpleNamedElement
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.NamedElement
import org.jivesoftware.smack.util.XmlStringBuilder
import org.jxmpp.jid.Jid

class ChangeGroupchatMemberPreferencesIQ(val jid: Jid, private val memberId: String, val badge: String? = null,
                                         val nickname: String? = null) : GroupchatAbstractMembersIQ() {

    constructor(groupChat:GroupChat, memberId: String, badge: String? = null, nickname: String? = null)
            : this(groupChat.fullJidIfPossible ?: groupChat.contactJid.jid, memberId, badge, nickname)

    init {
        this.type = Type.set
        this.to = jid
    }

    companion object {
        const val BADGE_ELEMENT = "badge"
        const val NICKNAME_ELEMENT = "nickname"
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        val elementsList = mutableListOf<NamedElement>()

        if (badge == null && nickname.isNullOrBlank())
            throw IllegalStateException("There are should be at least one argument")

        if (badge != null) elementsList.add(SimpleNamedElement(BADGE_ELEMENT, badge))
        if (!nickname.isNullOrBlank()) elementsList.add(SimpleNamedElement(NICKNAME_ELEMENT, nickname))

        append(UserExtensionElement(memberId, elementsList).toXML())
    }

    private class UserExtensionElement(val memberId: String, val elements: List<NamedElement>) : ExtensionElement {

        companion object {
            const val USER_ELEMENT = "user"
            const val ID_ATTRIBUTE = "id"
        }

        override fun getNamespace() = GroupchatManager.NAMESPACE

        override fun getElementName() = USER_ELEMENT

        override fun toXML() = XmlStringBuilder().apply {
            halfOpenElement(elementName)
            attribute(ID_ATTRIBUTE, memberId)
            xmlnsAttribute(namespace)
            rightAngleBracket()
            for (element in elements)
                append(element.toXML())
            closeElement(USER_ELEMENT)
        }

    }

}