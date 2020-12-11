package com.xabber.android.data.extension.groupchat.settings

import com.xabber.android.data.extension.groupchat.GroupchatAbstractQueryIQ
import com.xabber.android.data.message.chat.groupchat.GroupChat
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