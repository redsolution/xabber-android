package com.xabber.xmpp.devices

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceVORevokeExtensionElementTest {

    @Test
    fun `test toXml()`() {
        val reference = "<revoke xmlns='https://xabber.com/protocol/auth-tokens'><token-uid>firstUid</token-uid><token-uid>secondUid</token-uid></revoke>"
        val element = DeviceRevokeExtensionElement(listOf("firstUid", "secondUid"))
        assertEquals(reference, element.toXML().toString())
    }

}