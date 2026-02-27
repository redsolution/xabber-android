package com.xabber.xmpp.retract.outgoing

import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import org.jivesoftware.smack.packet.Message
import org.junit.Assert
import org.junit.Test

class ReplaceMessageIqTest {

    @Test
    fun `test toXml() without special archive address`() {

        val message = Message()

        val iq = ReplaceMessageIq(
            "stanzaId",
            AccountJid.from("account@domain.do/resource"),
            message
        )

        iq.stanzaId = "iqId"
        message.stanzaId = "messageId"

        val reference = "<iq id='iqId' type='set'>" +
                            "<replace xmlns='https://xabber.com/protocol/rewrite' by='account@domain.do' id='stanzaId'>" +
                                "<message id='messageId'></message>" +
                            "</replace>" +
                        "</iq>"

        Assert.assertEquals(reference, iq.toXML().toString())
    }

    @Test
    fun `test toXml() with special archive address`() {

        val message = Message()

        val iq = ReplaceMessageIq(
            "stanzaId",
            AccountJid.from("account@domain.do/resource"),
            message,
            ContactJid.from("archive@address.domain")
        )

        iq.stanzaId = "iqId"
        message.stanzaId = "messageId"

        val reference = "<iq to='archive@address.domain' id='iqId' type='set'>" +
                            "<replace xmlns='https://xabber.com/protocol/rewrite' by='account@domain.do' id='stanzaId'>" +
                                "<message id='messageId'></message>" +
                            "</replace>" +
                        "</iq>"

        Assert.assertEquals(reference, iq.toXML().toString())
    }

}