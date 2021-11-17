package com.xabber.xmpp.devices

import com.xabber.android.data.extension.devices.DevicesManager
import org.jivesoftware.smack.packet.IQ

class IncomingNewDeviceIQ(
    val secret: String, val uid: String, val expire: Long
) : IQ(ELEMENT, NAMESPACE) {

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        attribute(ATTRIBUTE_UID, uid)
        rightAngleBracket()
        element(ELEMENT_SECRET, secret)
        element(ELEMENT_EXPIRE, expire.toString())
    }

    companion object {
        const val NAMESPACE = DevicesManager.NAMESPACE
        const val ELEMENT = "device"
        const val ELEMENT_SECRET = "secret"
        const val ATTRIBUTE_UID = "id"
        const val ELEMENT_EXPIRE = "expire"
    }

}