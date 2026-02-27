package com.xabber.xmpp.retract.incoming.elements

import org.junit.Assert
import org.junit.Test

class ReplacedExtensionElementTest {

    @Test
    fun `test toXml()`() {
        val element = ReplacedExtensionElement("23.09.2021(lol)")

        val reference = "<replaced xmlns='https://xabber.com/protocol/rewrite' stamp='23.09.2021(lol)'/>"

        Assert.assertEquals(reference, element.toXML().toString())
    }
}