package com.xabber.android.data.extension.delivery

import com.xabber.xmpp.sid.OriginIdElement
import com.xabber.xmpp.sid.StanzaIdElement
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceivedExtensionElementTest {

    @Test
    fun toXml_test(){
        val receivedExtensionElement = ReceivedExtensionElement()

        val referenceEmpty = "<received xmlns='https://xabber.com/protocol/delivery'></received>"
        assertEquals(referenceEmpty, receivedExtensionElement.toXML().toString())

        receivedExtensionElement.timeElement = TimeElement("juliet@capuliet.it", "2020-03-16T11:46:33.970745Z")
        val referenceWithTimeElement = "<received xmlns='https://xabber.com/protocol/delivery'><time by='juliet@capuliet.it' stamp='2020-03-16T11:46:33.970745Z'/></received>"

        assertEquals(referenceWithTimeElement, receivedExtensionElement.toXML().toString())

        receivedExtensionElement.stanzaIdElement = StanzaIdElement("juliet@capuliet.it", "1584359193970745")
        receivedExtensionElement.originIdElement = OriginIdElement("fa20384a-75ea-4d4e-bb39-49e0fd55473b")
        val referenceWithAllElements = "<received xmlns='https://xabber.com/protocol/delivery'><time by='juliet@capuliet.it' stamp='2020-03-16T11:46:33.970745Z'/><origin-id xmlns='urn:xmpp:sid:0' id='fa20384a-75ea-4d4e-bb39-49e0fd55473b'/><stanza-id xmlns='urn:xmpp:sid:0' by='juliet@capuliet.it' id='1584359193970745'/></received>"

        assertEquals(referenceWithAllElements, receivedExtensionElement.toXML().toString())
    }

}