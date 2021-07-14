package com.xabber.xmpp.retract.incoming.elements

import com.xabber.android.data.entity.ContactJid
import org.junit.Assert
import org.junit.Test

class IncomingRetractExtensionElementTest {

    @Test
    fun `test toXml()`() {
        val element = IncomingRetractExtensionElement(
            "messageId",
            ContactJid.from("contact@jid.do"),
            "someVersion"
        )

        val reference = "<retract-message " +
                            "xmlns='https://xabber.com/protocol/rewrite#notify'" +
                            " version='someVersion'" +
                            " by='contact@jid.do' " +
                            "id='messageId'" +
                        "/>"

        Assert.assertEquals(reference, element.toXML().toString())
    }
}