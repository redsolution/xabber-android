package com.xabber.android.data.extension.groupchat.status

import com.xabber.android.data.entity.ContactJid

class GroupStatusFormRequestIQ(contactJid: ContactJid): AbstractGroupStatusIQ() {

    init {
        to = contactJid.bareJid
        type = Type.get
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
    }

}