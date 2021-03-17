package com.xabber.xmpp.groups.create

import com.xabber.android.data.extension.groups.GroupIndexType
import com.xabber.android.data.extension.groups.GroupMembershipType
import com.xabber.android.data.extension.groups.GroupPrivacyType
import com.xabber.xmpp.SimpleNamedElement
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.NamedElement
import org.jivesoftware.smack.util.XmlStringBuilder
import org.jxmpp.jid.Jid

class CreateGroupchatIQ(val contact: Jid, to: String, val groupName: String?, private val groupLocalpart: String?,
                        val description: String?, private val membershipType: GroupMembershipType,
                        private val privacyType: GroupPrivacyType, private val indexType: GroupIndexType)
    : GroupchatCreateAbstractIQ() {

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()

        append(SimpleNamedElement(NAME_ELEMENT, groupName!!).toXML())
        append(SimpleNamedElement(DESCRIPTION_ELEMENT, description!!).toXML())
        append(SimpleNamedElement(MEMBERSHIP_ELEMENT, membershipType.toXml()).toXML())
        append(SimpleNamedElement(PRIVACY_ELEMENT, privacyType.toXml()).toXML())
        append(SimpleNamedElement(INDEX_ELEMENT, indexType.toXml()).toXML())

        append(ContactsNamedElement(contact.asBareJid().toString()).toXML())

        if (groupLocalpart != null && groupLocalpart.isNotEmpty())
            append(SimpleNamedElement(LOCALPART_ELEMENT, groupLocalpart).toXML())

    }

    private class ContactsNamedElement(val contactJid: String) : NamedElement {

        companion object {
            private const val CONTACTS_ELEMENT = "contacts"
            private const val CONTACT_ELEMENT = "contact"
        }

        override fun getElementName() = CONTACTS_ELEMENT

        override fun toXML() = XmlStringBuilder(this).apply {
            rightAngleBracket()
            append(SimpleNamedElement(CONTACT_ELEMENT, contactJid).toXML())
            closeElement(CONTACTS_ELEMENT)
        }
    }

    companion object {
        const val HASH_BLOCK = "#create"
        const val ELEMENT_LOCALPART = "localpart"
        private const val NAME_ELEMENT = "name"
        private const val DESCRIPTION_ELEMENT = "description"
        private const val LOCALPART_ELEMENT = "localpart"
        private const val PRIVACY_ELEMENT = "privacy"
        private const val INDEX_ELEMENT = "index"
        private const val MEMBERSHIP_ELEMENT = "membership"
    }

    init {
        this.type = Type.set
        this.from = contact
        this.setTo(to)
    }

    /**
     * WARN! This is not a fully implemented class
     */
    class ResultIq(val localpart: String) : IQ(ELEMENT, NAMESPACE + HASH_BLOCK){
        val jid: String
            get() = "$localpart@$from"

        override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder?) = xml
    }

}