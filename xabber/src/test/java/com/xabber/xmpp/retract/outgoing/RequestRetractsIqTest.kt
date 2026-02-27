package com.xabber.xmpp.retract.outgoing

import com.xabber.android.data.entity.ContactJid
import org.junit.Assert
import org.junit.Test

class RequestRetractsIqTest {

    @Test
    fun `test toXml() with special version and with special archive address without limitations`() {
        val iq = RequestRetractsIq(
            archiveAddress = ContactJid.from("archive@address.co"),
            version = "someVersion",
            lessThan = 10
        )

        iq.stanzaId = "iqId"

        val reference = "<iq to='archive@address.co' id='iqId' type='get'>" +
                            "<query xmlns='https://xabber.com/protocol/rewrite' version='someVersion' less-than='10'></query>" +
                        "</iq>"
        Assert.assertEquals(reference, iq.toXML().toString())
    }

    @Test
    fun `test toXml() without anything`() {
        val iq = RequestRetractsIq()

        iq.stanzaId = "iqId"

        val reference = "<iq id='iqId' type='get'>" +
                            "<query xmlns='https://xabber.com/protocol/rewrite' less-than='100'></query>" +
                        "</iq>"
        Assert.assertEquals(reference, iq.toXML().toString())
    }

}