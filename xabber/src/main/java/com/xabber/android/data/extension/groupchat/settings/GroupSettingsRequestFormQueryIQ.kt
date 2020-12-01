package com.xabber.android.data.extension.groupchat.settings

import com.xabber.android.data.extension.groupchat.GroupchatAbstractQueryIQ
import com.xabber.android.data.message.chat.groupchat.GroupChat

class GroupSettingsRequestFormQueryIQ(groupchat: GroupChat) : GroupchatAbstractQueryIQ(NAMESPACE) {

    init {
        to = groupchat.fullJidIfPossible ?: groupchat.contactJid.jid
        type = Type.get
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
    }

}