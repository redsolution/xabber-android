package com.xabber.xmpp.devices

import com.xabber.android.data.extension.devices.DevicesManager
import com.xabber.xmpp.SimpleNamedElement
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.util.XmlStringBuilder
import org.jxmpp.jid.DomainBareJid

class ChangeDeviceDescriptionIQ(
    serverAddress: DomainBareJid,
    private val deviceUid: String,
    private val description: String
): IQ(ELEMENT_NAME, DevicesManager.NAMESPACE) {

    init {
        type = Type.set
        to = serverAddress
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder?) = xml?.apply {
        rightAngleBracket()
        optElement(
            DeviceChangeDescriptionElement(deviceUid, description)
        )
    }

    private companion object {
        const val ELEMENT_NAME = "query"
    }

    private class DeviceChangeDescriptionElement(
        private val uid: String,
        private val description: String
    ): ExtensionElement {

        override fun getElementName() = ELEMENT_NAME

        override fun getNamespace() = DevicesManager.NAMESPACE

        override fun toXML() = XmlStringBuilder().apply {
            halfOpenElement(ELEMENT_NAME)
            attribute(ELEMENT_UID_ATTRIBUTE, uid)
            rightAngleBracket()
            append(
                SimpleNamedElement(ELEMENT_DESCRIPTION, description).toXML()
            )
            closeElement(ELEMENT_NAME)
        }

        private companion object {
            const val ELEMENT_NAME = "device"
            const val ELEMENT_UID_ATTRIBUTE = "id"
            const val ELEMENT_DESCRIPTION = "description"
        }
    }

}