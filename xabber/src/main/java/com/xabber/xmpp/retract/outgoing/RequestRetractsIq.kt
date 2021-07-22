package com.xabber.xmpp.retract.outgoing

import com.xabber.android.data.entity.ContactJid

class RequestRetractsIq(
    private val version: String? = null,
    archiveAddress: ContactJid? = null,
    private val lessThan: Int? = 250,
) : AbstractRetractIq(archiveAddress, ELEMENT) {

    init {
        archiveAddress?.let { to = archiveAddress.jid }
        type = Type.get
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        version?.let { attribute(VERSION_ATTRIBUTE, it) }
        lessThan?.let { attribute(LESS_THAN_ATTRIBUTE, it) }
        rightAngleBracket()
    }

    private companion object {
        const val ELEMENT = "query"
        const val VERSION_ATTRIBUTE = "version"
        const val LESS_THAN_ATTRIBUTE = "less-than"
    }

}