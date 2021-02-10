package com.xabber.xmpp.vcard

import com.xabber.android.data.groups.GroupIndexType
import com.xabber.android.data.groups.GroupMembershipType
import com.xabber.android.data.groups.GroupPrivacyType
import com.xabber.xmpp.SimpleNamedElement
import org.jivesoftware.smackx.vcardtemp.packet.VCard
import java.util.*

open class VCard : VCard() {
    /**
     * Phone types:
     * VOICE?, FAX?, PAGER?, MSG?, CELL?, VIDEO?, BBS?, MODEM?, ISDN?, PCS?, PREF?
     */
    private val mobilePhones: MutableMap<String, String> = HashMap()

    /**
     * Set work phone number.
     *
     * @param phoneType one of VOICE, FAX, PAGER, MSG, CELL, VIDEO, BBS, MODEM, ISDN, PCS, PREF
     * @param phoneNum  phone number
     */
    fun setPhoneMobile(phoneType: String, phoneNum: String) {
        if (phoneType != null && phoneNum != null && mobilePhones != null) mobilePhones[phoneType] = phoneNum
    }

    var privacyType = GroupPrivacyType.NONE
    var indexType = GroupIndexType.NONE
    var membershipType = GroupMembershipType.NONE

    var description = ""
    var membersCount = 0

    /**
     * Get work phone number.
     *
     * @param phoneType one of VOICE, FAX, PAGER, MSG, CELL, VIDEO, BBS, MODEM, ISDN, PCS, PREF
     */
    fun getPhoneMobile(phoneType: String): String? {
        return mobilePhones[phoneType]
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        if (hasContent()) {
            rightAngleBracket()
            for ((_, value) in mobilePhones) {
                val number = value
                openElement("TEL")
                //xml.emptyElement(phone.getKey());
                element("NUMBER", number)
                //xml.closeElement("TEL");
                append("</TEL")
            }
            append(SimpleNamedElement(PRIVACY_ELEMENT, privacyType.toXml()).toXML())
            append(SimpleNamedElement(INDEX_ELEMENT, indexType.toXml()).toXML())
            append(SimpleNamedElement(MEMBERSHIP_ELEMENT, membershipType.toXml()).toXML())
            append(SimpleNamedElement(DESCRIPTION_ELEMENT, description).toXML())
            append(SimpleNamedElement(MEMBERS_ELEMENT, membersCount.toString()).toXML())
        }
        super.getIQChildElementBuilder(xml)
        return xml
    }

    private fun hasContent() = mobilePhones.isNotEmpty()

    companion object{
        const val PRIVACY_ELEMENT = "X-PRIVACY"
        const val INDEX_ELEMENT = "X-INDEX"
        const val MEMBERSHIP_ELEMENT = "X-MEMBERSHIP"

        const val DESCRIPTION_ELEMENT = "DESC"
        const val MEMBERS_ELEMENT = "X-MEMBERS"
    }

}