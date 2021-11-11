package com.xabber.xmpp.xtoken

import org.junit.Assert.assertEquals
import org.junit.Test

class XTokenRevokeExtensionElementTest {

    @Test
    fun `test toXml()`() {
        val reference = "<revoke xmlns='https://xabber.com/protocol/auth-tokens'><token-uid>firstUid</token-uid><token-uid>secondUid</token-uid></revoke>"
        val element = XTokenRevokeExtensionElement(listOf("firstUid", "secondUid"))
        assertEquals(reference, element.toXML().toString())
    }

}