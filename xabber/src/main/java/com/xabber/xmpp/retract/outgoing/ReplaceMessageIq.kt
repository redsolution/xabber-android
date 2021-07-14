package com.xabber.xmpp.retract.outgoing

import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import org.jivesoftware.smack.packet.Message

class ReplaceMessageIq(
    private val messageStanzaId: String,
    private val accountJid: AccountJid,
    private val message: Message,
    archiveAddress: ContactJid? = null
) : AbstractRetractIq(archiveAddress, ELEMENT) {

    init {
        type = Type.set
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        attribute(BY_ATTRIBUTE, accountJid.bareJid.toString())
        attribute(ID_ATTRIBUTE, messageStanzaId)
        rightAngleBracket()
        append(message.toXML())
    }

    private companion object {
        const val ELEMENT = "replace"
        const val BY_ATTRIBUTE = "by"
        const val ID_ATTRIBUTE = "id"
    }

}