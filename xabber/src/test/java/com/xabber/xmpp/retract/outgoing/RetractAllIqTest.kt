package com.xabber.xmpp.retract.outgoing

import com.xabber.android.data.entity.ContactJid
import org.junit.Assert
import org.junit.Test

class RetractAllIqTest {

    @Test
    fun `test toXml() symmetrically without special archive address`() {

        val iq = RetractAllIq(
            ContactJid.from("contact@domain.do"),
            true
        )

        iq.stanzaId = "iqId"

        val reference = "<iq id='iqId' type='set'>" +
                            "<retract-all xmlns='https://xabber.com/protocol/rewrite' symmetric='true' conversation='contact@domain.do'></retract-all>" +
                        "</iq>"

        Assert.assertEquals(reference, iq.toXML().toString())
    }

    @Test
    fun `test toXml() non-symmetrically without special archive address`() {

        val iq = RetractAllIq(
            ContactJid.from("contact@domain.do")
        )

        iq.stanzaId = "iqId"

        val reference = "<iq id='iqId' type='set'>" +
                            "<retract-all xmlns='https://xabber.com/protocol/rewrite' symmetric='false' conversation='contact@domain.do'></retract-all>" +
                        "</iq>"

        Assert.assertEquals(reference, iq.toXML().toString())
    }

    @Test
    fun `test toXml() symmetrically with special archive address`() {

        val iq = RetractAllIq(
            ContactJid.from("contact@domain.do"),
            true,
            ContactJid.from("group@server.co")
        )

        iq.stanzaId = "iqId"

        val reference = "<iq to='group@server.co' id='iqId' type='set'>" +
                            "<retract-all " +
                                "xmlns='https://xabber.com/protocol/rewrite' " +
                                "symmetric='true' " +
                                "conversation='contact@domain.do'>" +
                            "</retract-all>" +
                        "</iq>"

        Assert.assertEquals(reference, iq.toXML().toString())
    }

    @Test
    fun `test toXml() non-symmetrically with special archive address`() {

        val iq = RetractAllIq(
            contactJid = ContactJid.from("contact@domain.do"),
            archiveAddress = ContactJid.from("group@server.co")
        )

        iq.stanzaId = "iqId"

        val reference = "<iq to='group@server.co' id='iqId' type='set'>" +
                            "<retract-all xmlns='https://xabber.com/protocol/rewrite' symmetric='false' conversation='contact@domain.do'></retract-all>" +
                        "</iq>"

        Assert.assertEquals(reference, iq.toXML().toString())
    }

}