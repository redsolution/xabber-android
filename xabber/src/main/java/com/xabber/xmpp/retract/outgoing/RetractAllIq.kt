package com.xabber.xmpp.retract.outgoing

import com.xabber.android.data.entity.ContactJid

class RetractAllIq(
    private val contactJid: ContactJid,
    private val symmetrically: Boolean = false,
    archiveAddress: ContactJid? = null,
) : AbstractRetractIq(archiveAddress, ELEMENT) {

    init {
        type = Type.set
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        attribute(SYMMETRIC_ATTRIBUTE, symmetrically)
        attribute(CONVERSATION_ATTRIBUTE, contactJid.bareJid.toString())
        rightAngleBracket()
    }

    private companion object {
        const val ELEMENT = "retract-all"
        const val SYMMETRIC_ATTRIBUTE = "symmetric"
        const val CONVERSATION_ATTRIBUTE = "conversation"
    }

}