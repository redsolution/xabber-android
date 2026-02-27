package com.xabber.xmpp.groups.status

import com.xabber.android.data.message.chat.GroupChat

class GroupStatusFormRequestIQ(groupchat: GroupChat): AbstractGroupStatusIQ() {

    init {
        to = groupchat.fullJidIfPossible ?: groupchat.contactJid.jid
        type = Type.get
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
    }

}