package com.xabber.xmpp.groups.restrictions

import com.xabber.android.data.message.chat.GroupChat

class RequestGroupDefaultRestrictionsDataFormIQ(groupchat: GroupChat): AbstractGroupDefaultRestrictionsIQ() {

    init {
        type = Type.get
        to = groupchat.fullJidIfPossible ?: groupchat.contactJid.jid
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        xml.setEmptyElement()
    }

}