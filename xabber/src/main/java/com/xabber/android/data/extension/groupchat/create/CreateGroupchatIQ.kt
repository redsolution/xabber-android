package com.xabber.android.data.extension.groupchat.create

import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.message.chat.groupchat.GroupchatIndexType
import com.xabber.android.data.message.chat.groupchat.GroupchatManager
import com.xabber.android.data.message.chat.groupchat.GroupchatMembershipType
import com.xabber.android.data.message.chat.groupchat.GroupchatPrivacyType
import com.xabber.xmpp.SimpleNamedElement
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.NamedElement
import org.jivesoftware.smack.util.XmlStringBuilder
import org.jxmpp.jid.Jid
import java.util.*

class CreateGroupchatIQ(from: Jid, to: String, groupName: String?, groupLocalpart: String?,
                        description: String?, membershipType: GroupchatMembershipType,
                        privacyType: GroupchatPrivacyType, indexType: GroupchatIndexType) : IQ(QUERY_ELEMENT, NAMESPACE) {
    private val to: String
    private val from: Jid
    private val elements: MutableCollection<NamedElement> = ArrayList()
    override fun getFrom(): Jid {
        return from
    }

    override fun getType(): Type {
        return Type.set
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder): IQChildElementXmlStringBuilder {
        xml.rightAngleBracket()
        if (elements.size != 0) for (element in elements) xml.append(element.toXML())
        return xml
    }

    private class MSimpleParentElement : NamedElement {
        var elementName: String
        var nestedElements: MutableList<NamedElement> = ArrayList()

        internal constructor(elementName: String, nestedElements: List<NamedElement>?) {
            this.elementName = elementName
            this.nestedElements.addAll(nestedElements!!)
        }

        internal constructor(elementName: String, nestedElement: NamedElement) {
            this.elementName = elementName
            nestedElements.add(nestedElement)
        }

        override fun getElementName(): String {
            return elementName
        }

        override fun toXML(): CharSequence {
            val xmlStringBuilder = XmlStringBuilder(this)
            xmlStringBuilder.rightAngleBracket()
            for (nestedElement in nestedElements) xmlStringBuilder.append(nestedElement.toXML())
            xmlStringBuilder.closeElement(this)
            return xmlStringBuilder
        }
    }

    interface CreateGroupchatIqResultListener {
        fun onJidConflict()
        fun onOtherError()
        fun onSuccessfullyCreated(accountJid: AccountJid?, contactJid: ContactJid?)
    }

    companion object {
        const val HASH_BLOCK = "#create"
        const val NAMESPACE = GroupchatManager.NAMESPACE + HASH_BLOCK
        const val QUERY_ELEMENT = "query"
        const val JID_ELEMENT = "jid"
        private const val NAME_ELEMENT = "name"
        private const val DESCRIPTION_ELEMENT = "description"
        private const val LOCALPART_ELEMENT = "localpart"
        private const val PRIVACY_ELEMENT = "privacy"
        private const val INDEX_ELEMENT = "index"
        private const val MEMBERSHIP_ELEMENT = "membership"
        private const val CONTACTS_ELEMENT = "contacts"
        private const val CONTACT_ELEMENT = "contact"
    }

    init {
        type = Type.set
        this.setFrom(from)
        this.setTo(to)
        elements.add(SimpleNamedElement(NAME_ELEMENT, groupName!!))
        elements.add(SimpleNamedElement(DESCRIPTION_ELEMENT, description!!))
        elements.add(SimpleNamedElement(MEMBERSHIP_ELEMENT, membershipType.toXml()!!))
        elements.add(SimpleNamedElement(PRIVACY_ELEMENT, privacyType.toXml()!!))
        elements.add(SimpleNamedElement(INDEX_ELEMENT, indexType.toXml()!!))
        elements.add(MSimpleParentElement(CONTACTS_ELEMENT,
                SimpleNamedElement(CONTACT_ELEMENT, from.asBareJid().toString())))
        if (groupLocalpart != null && !groupLocalpart.isEmpty()) elements.add(SimpleNamedElement(LOCALPART_ELEMENT, groupLocalpart))
        this.from = from
        this.to = to
    }
}