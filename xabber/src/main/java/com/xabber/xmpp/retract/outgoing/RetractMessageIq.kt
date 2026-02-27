package com.xabber.xmpp.retract.outgoing

import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid

class RetractMessageIq(
    private val messageStanzaId: String,
    private val accountJid: AccountJid,
    private val symmetrically: Boolean = false,
    archiveAddress: ContactJid? = null,
) : AbstractRetractIq(archiveAddress, ELEMENT) {

    init {
        type = Type.set
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        attribute(SYMMETRIC_ATTRIBUTE, symmetrically)
        attribute(BY_ATTRIBUTE, accountJid.bareJid.toString())
        attribute(ID_ATTRIBUTE, messageStanzaId)
        rightAngleBracket()
    }

    private companion object {
        const val ELEMENT = "retract-message"
        const val SYMMETRIC_ATTRIBUTE = "symmetric"
        const val BY_ATTRIBUTE = "by"
        const val ID_ATTRIBUTE = "id"
    }

}