package com.xabber.android.data.extension.groupchat.rights

import com.xabber.android.data.entity.ContactJid
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.util.XmlStringBuilder

internal class GroupchatMemberRightsQueryIQ(groupchatJid: ContactJid, private val memberId: String)
    : GroupchatAbstractRightsIQ() {

    init {
        type = Type.get
        to = groupchatJid.bareJid
    }

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