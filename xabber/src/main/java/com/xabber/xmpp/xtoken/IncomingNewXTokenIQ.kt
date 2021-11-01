package com.xabber.xmpp.xtoken

import com.xabber.android.data.extension.xtoken.XTokenManager
import org.jivesoftware.smack.packet.IQ

class IncomingNewXTokenIQ(
    val token: String, val uid: String, val expire: Long
) : IQ(ELEMENT, NAMESPACE) {

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        attribute(ATTRIBUTE_UID, uid)
        rightAngleBracket()
        element(ELEMENT_TOKEN, token)
        element(ELEMENT_EXPIRE, expire.toString())
    }

    companion object {
        const val NAMESPACE = XTokenManager.NAMESPACE
        const val ELEMENT = "xtoken"
        const val ELEMENT_TOKEN = "token"
        const val ATTRIBUTE_UID = "uid"
        const val ELEMENT_EXPIRE = "expire"
    }

}