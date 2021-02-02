package com.xabber.android.data.extension.groupchat.block

import com.xabber.android.data.groups.GroupsManager
import org.jivesoftware.smack.packet.IQ
import org.jxmpp.jid.FullJid
import org.jxmpp.jid.Jid

class KickGroupMemberIQ private constructor(groupChatFullJid: FullJid) : IQ(ELEMENT, NAMESPACE) {

    private var jid: Jid? = null
    private var memberId: String? = null

    init {
        to = groupChatFullJid
        type = Type.set
    }

    constructor(jid: Jid, groupChatFullJid: FullJid) : this(groupChatFullJid) {
        this.jid = jid
    }

    constructor(memberId: String, groupChatFullJid: FullJid) : this(groupChatFullJid) {
        this.memberId = memberId
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        if (!memberId.isNullOrEmpty()) {
            optElement(ID_ELEMENT, memberId)
        } else if (jid != null) {
            optElement(JID_ELEMENT, jid)
        } else throw IllegalArgumentException()
    }

    private companion object {
        const val NAMESPACE = GroupsManager.NAMESPACE
        const val ELEMENT = "kick"
        const val JID_ELEMENT = "jid"
        const val ID_ELEMENT = "id"
    }

}