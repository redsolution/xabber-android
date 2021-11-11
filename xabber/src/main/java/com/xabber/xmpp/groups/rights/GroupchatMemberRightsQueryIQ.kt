package com.xabber.xmpp.groups.rights

import com.xabber.android.data.message.chat.GroupChat
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.util.XmlStringBuilder
import org.jxmpp.jid.Jid

internal class GroupchatMemberRightsQueryIQ(jid: Jid, private val memberId: String)
    : GroupchatAbstractRightsIQ() {

    init {
        type = Type.get
        to = jid
    }

    constructor(groupChat: GroupChat, memberId: String)
            : this(groupChat.fullJidIfPossible ?: groupChat.contactJid.jid, memberId)

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(MemberIdExtensionElement(memberId).toXML())
    }

    private class MemberIdExtensionElement(val memberId: String) : ExtensionElement{

        companion object{
            const val ATTRIBUTE_NAME = "id"
        }

        override fun getNamespace(): String = NAMESPACE

        override fun getElementName(): String = "user"

        override fun toXML() = XmlStringBuilder(this).apply {
            attribute(ATTRIBUTE_NAME, memberId)
            rightAngleBracket()
            closeElement(elementName)
        }

    }

}