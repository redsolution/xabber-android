package com.xabber.xmpp.ccc

import com.xabber.android.data.entity.ContactJid
import org.junit.Assert.assertEquals
import org.junit.Test

class IncomingSetSyncIqTest {

    @Test
    fun deletedElement_test(){
        assertEquals("<deleted/>", DeletedElement().toXML().toString())
    }

    @Test
    fun conversationExtensionElement_test(){
        assertEquals("<conversation jid='jidofconversation@server.do'><deleted/></conversation>",
                ConversationExtensionElement(ContactJid.from("jidofconversation@server.do"), DeletedElement())
                        .toXML()
                        .toString())
    }

    @Test
    fun incomingSetSyncIq_test(){
        val iq = IncomingSetSyncIQ(
                1000000L,
                ConversationExtensionElement(ContactJid.from("jidofconversation@server.do"), DeletedElement())
        )
        iq.stanzaId = "stanzaId"

        val reference =
                "<iq id='stanzaId' type='set'>" +
                    "<query xmlns='https://xabber.com/protocol/synchronization' stamp='1000000'>" +
                        "<conversation jid='jidofconversation@server.do'>" +
                            "<deleted/>" +
                        "</conversation>" +
                    "</query>" +
                "</iq>"

        assertEquals(reference, iq.toXML().toString())
    }

}