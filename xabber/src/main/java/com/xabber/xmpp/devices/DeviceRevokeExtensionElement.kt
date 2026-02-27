package com.xabber.xmpp.devices

import com.xabber.android.data.extension.devices.DevicesManager
import com.xabber.xmpp.SimpleNamedElement
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.util.XmlStringBuilder

class DeviceRevokeExtensionElement(val ids: List<String>) : ExtensionElement {

    override fun getElementName() = ELEMENT_NAME

    override fun getNamespace() = DevicesManager.NAMESPACE

    override fun toXML() = XmlStringBuilder().apply {
        halfOpenElement(elementName)
        xmlnsAttribute(namespace)
        rightAngleBracket()
        append(ids.map { SimpleNamedElement(DEVICE_ELEMENT_NAME, it) })
        closeElement(elementName)
    }

    companion object {
        const val ELEMENT_NAME = "revoke"
        const val DEVICE_ELEMENT_NAME = "device"
        const val ID_ATTRIBUTE = "id"

        fun Stanza.hasDeviceRevokeExtensionElement() =
            this.hasExtension(ELEMENT_NAME, DevicesManager.NAMESPACE)

        fun Stanza.getDeviceRevokeExtensionElement(): DeviceRevokeExtensionElement =
            this.getExtension(ELEMENT_NAME, DevicesManager.NAMESPACE)
    }

}