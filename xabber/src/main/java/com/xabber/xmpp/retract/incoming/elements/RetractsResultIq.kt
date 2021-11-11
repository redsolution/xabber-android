package com.xabber.xmpp.retract.incoming.elements

import com.xabber.android.data.extension.retract.RetractManager
import org.jivesoftware.smack.packet.IQ

class RetractsResultIq(val version: String? = null) : IQ(ELEMENT, RetractManager.NAMESPACE) {

    init {
        type = Type.result
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        version?.let { attribute(VERSION_ATTRIBUTE, it) }
        rightAngleBracket()
    }

    companion object {
        const val ELEMENT = "query"
        const val VERSION_ATTRIBUTE = "version"
    }

}