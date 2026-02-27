package com.xabber.xmpp.devices

import junit.framework.TestCase
import org.jxmpp.jid.impl.JidCreate

class ChangeDeviceDescriptionIQTest : TestCase() {

    fun `testGetIQChildElementBuilder when createRegisterDeviceRequest`() {
        val reference = "<iq to='server.domain' id='SS21S-1' type='set'><register xmlns='https://xabber.com/protocol/devices'><device><client>Xabber Test v. 1.33.7</client><info>Xiaomi(top za svoi dengi)</info></device></register></iq>"

        val serverBareJid = JidCreate.domainBareFrom("server.domain")
        val testIq = DeviceRegisterIQ.createRegisterDeviceRequest(
            server = serverBareJid,
            client = "Xabber Test v. 1.33.7",
            info = "Xiaomi(top za svoi dengi)"
        ).apply {
            stanzaId = "SS21S-1"
        }
        assertEquals(reference, testIq.toXML().toString())
    }

    fun `testGetIQChildElementBuilder when createRequestNewSecretForDevice`() {
        val reference = "<iq to='server.domain' id='SS21S-1' type='set'><register xmlns='https://xabber.com/protocol/devices'><device id='someOldExistingDeviceId'/></register></iq>"
        val serverBareJid = JidCreate.domainBareFrom("server.domain")
        val testIq = DeviceRegisterIQ.createRequestNewSecretForDevice(
            server = serverBareJid,
            id = "someOldExistingDeviceId"
        ).apply {
            stanzaId = "SS21S-1"
        }
        assertEquals(reference, testIq.toXML().toString())
    }
}