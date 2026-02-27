package com.xabber.xmpp.mam

import junit.framework.TestCase
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smackx.forward.packet.Forwarded
import org.jxmpp.jid.impl.JidCreate

class MamResultExtensionElementTest : TestCase() {

    fun test_toXml(){
        val messageStanza = Message(
                JidCreate.bareFrom("jid@contact.domain"),
                "This is message body lorem ipsum"
        )
        messageStanza.stanzaId = "messageId"
        val forwarded = Forwarded(messageStanza)

        val reference1 = "<result xmlns='urn:xmpp:mam:2' id='resultId'><forwarded xmlns='urn:xmpp:forward:0'><message to='jid@contact.domain' id='messageId'><body>This is message body lorem ipsum</body></message></forwarded></result>"
        val mamResultExtensionElement1 = MamResultExtensionElement("resultId", forwarded)
        assertEquals("Error with creating element without query id",
                reference1,
                mamResultExtensionElement1.toXML().toString()
        )

        val reference2 = "<result xmlns='urn:xmpp:mam:2' queryid='queryId' id='resultId'><forwarded xmlns='urn:xmpp:forward:0'><message to='jid@contact.domain' id='messageId'><body>This is message body lorem ipsum</body></message></forwarded></result>"
        val mamResultExtensionElement2 = MamResultExtensionElement("resultId", forwarded, "queryId")
        assertEquals("Error with creating element with query id",
                reference2,
                mamResultExtensionElement2.toXML().toString()
        )
    }

}