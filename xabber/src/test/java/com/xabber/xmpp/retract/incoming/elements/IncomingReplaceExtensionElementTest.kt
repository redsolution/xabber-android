package com.xabber.xmpp.retract.incoming.elements

import com.xabber.android.data.entity.ContactJid
import org.jivesoftware.smack.packet.Message
import org.junit.Assert
import org.junit.Test

class IncomingReplaceExtensionElementTest {

    @Test
    fun `test toXml`() {
        val element = IncomingReplaceExtensionElement(
            messageStanzaId = "stanzaId",
            conversationContactJid = ContactJid.from("contact@server.domain"),
            message = Message().apply { stanzaId = "messageId" },
            version = "100500"
        )

        val reference = "<replace " +
                            "xmlns='https://xabber.com/protocol/rewrite#notify' " +
                            "id='stanzaId' " +
                            "by='contact@server.domain' " +
                            "version='100500'>" +
                                "<message id='messageId'></message>" +
                        "</replace>"

        Assert.assertEquals(reference, element.toXML().toString())
    }

}