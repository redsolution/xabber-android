package com.xabber.android.data.extension.groupchat.settings

import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.groupchat.GroupchatAbstractQueryIQ

class GroupSettingsRequestFormQueryIQ(groupchatJid: ContactJid)
    : GroupchatAbstractQueryIQ(NAMESPACE) {

    init {
        to = groupchatJid.jid
        type = Type.get
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
    }

}