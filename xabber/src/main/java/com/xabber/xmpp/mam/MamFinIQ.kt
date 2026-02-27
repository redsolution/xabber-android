package com.xabber.xmpp.mam

import com.xabber.android.data.extension.archive.MessageArchiveManager
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smackx.rsm.packet.RSMSet

class MamFinIQ(
    val rsmSet: RSMSet,
    val queryId: String? = null,
    val isComplete: Boolean? = null,
) : IQ(ELEMENT, NAMESPACE) {

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        queryId?.let { optAttribute(QUERY_ID_ATTRIBUTE, it) }
        isComplete?.let { optBooleanAttribute(COMPLETE_ATTRIBUTE, it) }
        rightAngleBracket()
        element(rsmSet)
    }

    companion object {
        const val ELEMENT = "fin"
        const val QUERY_ID_ATTRIBUTE = "queryid"
        const val COMPLETE_ATTRIBUTE = "complete"
        const val NAMESPACE = MessageArchiveManager.NAMESPACE
    }

}