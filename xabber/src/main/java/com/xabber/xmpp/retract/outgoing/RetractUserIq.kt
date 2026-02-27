package com.xabber.xmpp.retract.outgoing

import com.xabber.android.data.entity.ContactJid

class RetractUserIq(
    groupJid: ContactJid, val memberId: String
) : AbstractRetractIq(groupJid, ELEMENT_NAME) {

    init {
        type = Type.set
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder?) = xml?.apply {
        attribute(SYMMETRIC_ATTRIBUTE, true)
        attribute(ID_ATTRIBUTE, memberId)
        attribute(BY_ATTRIBUTE, to)
    }

    private companion object {
        const val ELEMENT_NAME = "retract-user"
        const val SYMMETRIC_ATTRIBUTE = "symmetric"
        const val ID_ATTRIBUTE = "id"
        const val BY_ATTRIBUTE = "by"
    }
}