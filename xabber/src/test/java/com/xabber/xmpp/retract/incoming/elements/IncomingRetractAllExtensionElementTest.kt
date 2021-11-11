package com.xabber.xmpp.retract.incoming.elements

import com.xabber.android.data.entity.ContactJid
import org.junit.Assert
import org.junit.Test

class IncomingRetractAllExtensionElementTest {

    @Test
    fun `test toXml()`() {
        val element = IncomingRetractAllExtensionElement(
            ContactJid.from("contact@jid.do"),
            "someVersion"
        )

        val reference = "<retract-all " +
                            "xmlns='https://xabber.com/protocol/rewrite#notify' " +
                            "version='someVersion' " +
                            "conversation='contact@jid.do'" +
                        "/>"

        Assert.assertEquals(reference, element.toXML().toString())
    }
}