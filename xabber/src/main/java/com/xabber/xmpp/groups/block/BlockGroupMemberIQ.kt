package com.xabber.xmpp.groups.block

import com.xabber.android.data.extension.groups.GroupsManager
import org.jivesoftware.smack.packet.IQ
import org.jxmpp.jid.FullJid
import org.jxmpp.jid.Jid

class BlockGroupMemberIQ private constructor(groupchatFullJid: FullJid) : IQ(ELEMENT, NAMESPACE) {

    private var memberId: String? = null
    private var memberJid: Jid? = null

    init {
        to = groupchatFullJid
        type = Type.set
    }

    constructor(groupchatFullJid: FullJid, memberId: String) : this(groupchatFullJid) {
        this.memberId = memberId
    }

    constructor(groupchatFullJid: FullJid, memberJid: Jid) : this(groupchatFullJid) {
        this.memberJid = memberJid
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        if (!memberId.isNullOrEmpty()) {
            optElement(ID_ELEMENT, memberId)
        } else if (memberJid != null) {
            optElement(JID_ELEMENT, memberJid)
        } else throw IllegalArgumentException()
    }

    private companion object {
        const val HASH_BLOCK = "#block"
        const val NAMESPACE = GroupsManager.NAMESPACE + HASH_BLOCK
        const val ELEMENT = "block"
        const val JID_ELEMENT = "jid"
        const val ID_ELEMENT = "id"
    }

}