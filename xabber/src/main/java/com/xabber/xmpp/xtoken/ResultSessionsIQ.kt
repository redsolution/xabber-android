package com.xabber.xmpp.xtoken

import com.xabber.android.data.extension.xtoken.XTokenManager
import org.jivesoftware.smack.packet.IQ

class ResultSessionsIQ(val sessions: List<Session>) : IQ(ELEMENT, NAMESPACE) {

    /**
     * Warn! Not implemented correctly!
     */
    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
    }

    data class Session(
        val client: String,
        val device: String,
        val uid: String,
        val ip: String,
        val expire: Long,
        val lastAuth: Long,
        val description: String,
    ) {

        companion object {
            const val ELEMENT_NAME = "xtoken"
            const val ELEMENT_CLIENT = "client"
            const val ELEMENT_DEVICE = "device"
            const val ELEMENT_UID_ATTRIBUTE = "uid"
            const val ELEMENT_EXPIRE = "expire"
            const val ELEMENT_IP = "ip"
            const val ELEMENT_DESCRIPTION = "description"
            const val ELEMENT_LAST_AUTH = "last-auth"
        }
    }

    companion object {
        const val ELEMENT = "query"
        private const val HASH_BLOCK = "#items"
        const val NAMESPACE = XTokenManager.NAMESPACE + HASH_BLOCK
    }

}