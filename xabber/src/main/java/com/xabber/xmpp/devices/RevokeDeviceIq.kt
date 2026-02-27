package com.xabber.xmpp.devices

import com.xabber.android.data.extension.devices.DevicesManager
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.util.XmlStringBuilder
import org.jxmpp.jid.DomainBareJid

class RevokeDeviceIq(
    server: DomainBareJid,
    private val ids: List<String>,
) : IQ(ELEMENT, NAMESPACE) {

    init {
        type = Type.set
        to = server
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        ids.forEach {
            element(DeviceElement(it))
        }
    }

    companion object {
        const val ELEMENT = "revoke"
        const val NAMESPACE = DevicesManager.NAMESPACE
    }

    private class DeviceElement(private val deviceUid: String): ExtensionElement {
        override fun toXML() = XmlStringBuilder().apply {
            halfOpenElement(ELEMENT_NAME)
            attribute(ID_ATTRIBUTE, deviceUid)
            closeEmptyElement()
        }

        override fun getNamespace(): String = NAMESPACE
        override fun getElementName() = ELEMENT_NAME

        companion object {
            const val ELEMENT_NAME = "device"
            const val ID_ATTRIBUTE = "id"
        }
    }

}