package com.xabber.xmpp.device

import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.util.XmlStringBuilder

class DeviceExtensionElement(val deviceId: String): ExtensionElement {

    override fun getElementName() = ELEMENT_NAME
    override fun getNamespace() = NAMESPACE

    override fun toXML() = XmlStringBuilder().apply {
        halfOpenElement(ELEMENT_NAME)
        attribute(UID_ATTRIBUTE, deviceId)
        xmlnsAttribute(NAMESPACE)
        closeEmptyElement()
    }

    companion object {
        const val ELEMENT_NAME = "device"
        const val NAMESPACE = "https://xabber.com/protocol/devices"
        const val UID_ATTRIBUTE = "id"
    }
}