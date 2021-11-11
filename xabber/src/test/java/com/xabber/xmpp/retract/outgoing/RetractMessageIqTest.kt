package com.xabber.xmpp.retract.outgoing

import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import org.junit.Assert
import org.junit.Test

class RetractMessageIqTest {

    @Test
    fun `test toXml() symmetrically without special archive address`() {
        val iq = RetractMessageIq(
            "stanzaId",
            AccountJid.from("account@domain.do/resource"),
            true
        )

        iq.stanzaId = "iqId"

        val reference = "<iq id='iqId' type='set'>" +
                            "<retract-message " +
                                "xmlns='https://xabber.com/protocol/rewrite' " +
                                "symmetric='true' " +
                                "by='account@domain.do' " +
                                "id='stanzaId'>" +
                            "</retract-message>" +
                        "</iq>"

        Assert.assertEquals(reference, iq.toXML().toString())
    }

    @Test
    fun `test toXml() non-symmetrically without special archive address`() {
        val iq = RetractMessageIq(
            "stanzaId",
            AccountJid.from("account@domain.do/resource")
        )

        iq.stanzaId = "iqId"

        val reference = "<iq id='iqId' type='set'>" +
                            "<retract-message " +
                                "xmlns='https://xabber.com/protocol/rewrite' " +
                                "symmetric='false' " +
                                "by='account@domain.do' " +
                                "id='stanzaId'>" +
                            "</retract-message>" +
                        "</iq>"

        Assert.assertEquals(reference, iq.toXML().toString())
    }

    @Test
    fun `test toXml() symmetrically with special archive address`() {
        val iq = RetractMessageIq(
            "stanzaId",
            AccountJid.from("account@domain.do/resource"),
            true,
            ContactJid.from("archive@address.co")
        )

        iq.stanzaId = "iqId"

        val reference = "<iq to='archive@address.co' id='iqId' type='set'>" +
                            "<retract-message " +
                                "xmlns='https://xabber.com/protocol/rewrite' " +
                                "symmetric='true' " +
                                "by='account@domain.do' " +
                                "id='stanzaId'>" +
                            "</retract-message>" +
                        "</iq>"

        Assert.assertEquals(reference, iq.toXML().toString())
    }

    @Test
    fun `test toXml() non-symmetrically with special archive address`() {
        val iq = RetractMessageIq(
            messageStanzaId = "stanzaId",
            accountJid = AccountJid.from("account@domain.do/resource"),
            archiveAddress = ContactJid.from("archive@address.co")
        )

        iq.stanzaId = "iqId"

        val reference = "<iq to='archive@address.co' id='iqId' type='set'>" +
                            "<retract-message " +
                                "xmlns='https://xabber.com/protocol/rewrite' " +
                                "symmetric='false' " +
                                "by='account@domain.do' " +
                                "id='stanzaId'>" +
                             "</retract-message>" +
                        "</iq>"

        Assert.assertEquals(reference, iq.toXML().toString())
    }

}