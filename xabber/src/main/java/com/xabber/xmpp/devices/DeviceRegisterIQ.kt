package com.xabber.xmpp.devices

import com.xabber.android.data.extension.devices.DevicesManager
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.util.XmlStringBuilder
import org.jxmpp.jid.DomainBareJid

class DeviceRegisterIQ private constructor(
    server: DomainBareJid,
    private val client: String? = null,
    private val info: String? = null,
    private val id: String? = null,
) : IQ(ELEMENT, NAMESPACE) {

    init {
        type = Type.set
        to = server
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(
            DeviceElement(client, info, id).toXML().toString()
        )
    }

    companion object {
        private const val ELEMENT = "register"
        private const val NAMESPACE = DevicesManager.NAMESPACE

        fun createRegisterDeviceRequest(
            server: DomainBareJid,
            client: String,
            info: String,
        ) = DeviceRegisterIQ(server, client, info)

        fun createRequestNewSecretForDevice(
            server: DomainBareJid,
            id: String,
        ) = DeviceRegisterIQ(server = server, id = id)
    }

    private class DeviceElement(private val client: String? = null,
                                private val info: String? = null,
                                private val id: String? = null,
    ): ExtensionElement {
        override fun toXML() = XmlStringBuilder().apply {
            halfOpenElement(ELEMENT_NAME)
            id?.let { attribute(ID_ATTRIBUTE, id) }

            if (client != null && info != null) {
                rightAngleBracket()
                optElement(ELEMENT_CLIENT, client)
                optElement(ELEMENT_INFO, info)
                closeElement(ELEMENT_NAME)
            } else {
                closeEmptyElement()
            }
        }

        override fun getElementName() = ELEMENT_NAME

        override fun getNamespace() = ""

        private companion object {
            const val ELEMENT_NAME = "device"
            const val ID_ATTRIBUTE = "id"
            const val ELEMENT_CLIENT = "client"
            const val ELEMENT_INFO = "info"
        }
    }

}