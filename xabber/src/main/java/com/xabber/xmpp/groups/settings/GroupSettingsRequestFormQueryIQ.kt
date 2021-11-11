package com.xabber.xmpp.groups.settings

import com.xabber.xmpp.groups.GroupchatAbstractQueryIQ
import com.xabber.android.data.message.chat.GroupChat
import org.jxmpp.jid.Jid

class GroupSettingsRequestFormQueryIQ(jid: Jid) : GroupchatAbstractQueryIQ(NAMESPACE) {

    init {
        to = jid
        type = Type.get
    }

    constructor(groupchat: GroupChat): this(groupchat.fullJidIfPossible ?: groupchat.contactJid.jid)

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
    }

}