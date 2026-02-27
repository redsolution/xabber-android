package com.xabber.xmpp.groups

import com.xabber.android.data.extension.groups.GroupIndexType
import com.xabber.android.data.extension.groups.GroupMembershipType
import com.xabber.android.data.extension.groups.GroupPrivacyType
import org.jivesoftware.smack.util.XmlStringBuilder

class GroupPresenceExtensionElement(
    val name: String? = null,
    val description: String? = null,
    val privacy: GroupPrivacyType? = null,
    val membership: GroupMembershipType? = null,
    val index: GroupIndexType? = null,
    val pinnedMessageId: String? = null,
    val isCollect: Boolean = false,
    val isP2p: Boolean = false,
    val status: String? = null,
    val presentMembers: Int = 0,
    val allMembers: Int = 0,
) : GroupExtensionElement() {


    override fun appendToXML(xml: XmlStringBuilder) {
        xml.apply {
            optElement(NAME, name)
            if (privacy != null) optElement(PRIVACY, privacy!!.toXml())
            if (membership != null) optElement(MEMBERSHIP, membership!!.toXml())
            if (index != null) optElement(INDEX, index!!.toXml())
            optElement(PINNED_MESSAGE, pinnedMessageId)
            optElement(COLLECT, if (isCollect) "yes" else "no")
            optElement(PEER_TO_PEER, if (isP2p) "true" else "false")
            optElement(STATUS, status)
            optElement(PRESENT, presentMembers)
            optElement(MEMBERS, allMembers)
        }
    }

    companion object {
        const val NAMESPACE = GroupExtensionElement.NAMESPACE
        const val NAME = "name"
        const val DESCRIPTION = "description"
        const val PRIVACY = "privacy"
        const val MEMBERSHIP = "membership"
        const val INDEX = "index"
        const val PINNED_MESSAGE = "pinned-message"
        const val COLLECT = "collect"
        const val PEER_TO_PEER = "peer-to-peer"
        const val STATUS = "status"
        const val PRESENT = "present"
        const val MEMBERS = "members"

        @JvmField
        val presenceFields = arrayOf(
            NAME, DESCRIPTION, PRIVACY, MEMBERSHIP, INDEX,
            PINNED_MESSAGE, COLLECT, PEER_TO_PEER, STATUS, PRESENT, MEMBERS
        )
    }

}