package com.xabber.android.data.extension.groupchat.restrictions

import com.xabber.android.data.entity.ContactJid

class RequestGroupDefaultRestrictionsDataFormIQ(to: ContactJid): AbstractGroupDefaultRestrictionsIQ() {

    init {
        type = Type.get
        setTo(to.bareJid.toString())
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        xml.setEmptyElement()
    }

}