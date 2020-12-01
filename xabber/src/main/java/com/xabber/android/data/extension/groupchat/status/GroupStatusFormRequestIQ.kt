package com.xabber.android.data.extension.groupchat.status

import com.xabber.android.data.message.chat.groupchat.GroupChat

class GroupStatusFormRequestIQ(groupchat: GroupChat): AbstractGroupStatusIQ() {

    init {
        to = groupchat.fullJidIfPossible ?: groupchat.contactJid.jid
        type = Type.get
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
    }

}