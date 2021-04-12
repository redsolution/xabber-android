package com.xabber.xmpp.mam

import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.message.chat.RegularChat
import junit.framework.TestCase
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class MamQueryIQTest : TestCase() {

    fun test_createMamRequestIqAllMessagesInChat_toXml(){
        val reference_regularChat = "<iq id='iqId' type='set'><query xmlns='urn:xmpp:mam:2'><x xmlns='jabber:x:data' type='submit'><field var='FORM_TYPE' type='hidden'><value>urn:xmpp:mam:2</value></field><field var='with'><value>regular@server.domain</value></field></x></query></iq>"
        val regularChat = Mockito.mock(RegularChat::class.java)
        `when`(regularChat.contactJid).thenReturn(ContactJid.from("regular@server.domain"))
        val mamIq_regularChat = MamQueryIQ.createMamRequestIqAllMessagesInChat(regularChat)
        mamIq_regularChat.stanzaId = "iqId"
        assertEquals("Error creating with regular chat!",
                reference_regularChat,
                mamIq_regularChat.toXML().toString())

        val reference_groupChat = "<iq to='group@server.domain' id='iqId' type='set'><query xmlns='urn:xmpp:mam:2'><x xmlns='jabber:x:data' type='submit'><field var='FORM_TYPE' type='hidden'><value>urn:xmpp:mam:2</value></field><field var='with'><value>group@server.domain</value></field></x></query></iq>"
        val groupChat = Mockito.mock(GroupChat::class.java)
        `when`(groupChat.contactJid).thenReturn(ContactJid.from("group@server.domain"))
        val mamIq_groupChat = MamQueryIQ.createMamRequestIqAllMessagesInChat(groupChat)
        mamIq_groupChat.stanzaId = "iqId"
        assertEquals("Error creating MAM IQ query to group chat!",
                reference_groupChat,
                mamIq_groupChat.toXML().toString())
    }

    fun test_createMamRequestIqLastMessageInChat_toXml(){
        val reference_regularChat = "<iq id='iqId' type='set'><query xmlns='urn:xmpp:mam:2'><x xmlns='jabber:x:data' type='submit'><field var='FORM_TYPE' type='hidden'><value>urn:xmpp:mam:2</value></field><field var='with'><value>regular@server.domain</value></field></x><set xmlns='http://jabber.org/protocol/rsm'><max>1</max></set></query></iq>"
        val regularChat = Mockito.mock(RegularChat::class.java)
        `when`(regularChat.contactJid).thenReturn(ContactJid.from("regular@server.domain"))
        val mamIq_regularChat = MamQueryIQ.createMamRequestIqLastMessageInChat(regularChat)
        mamIq_regularChat.stanzaId = "iqId"
        assertEquals("Error creating with regular chat!",
                reference_regularChat,
                mamIq_regularChat.toXML().toString())

        val reference_groupChat = "<iq to='group@server.domain' id='iqId' type='set'><query xmlns='urn:xmpp:mam:2'><x xmlns='jabber:x:data' type='submit'><field var='FORM_TYPE' type='hidden'><value>urn:xmpp:mam:2</value></field><field var='with'><value>group@server.domain</value></field></x><set xmlns='http://jabber.org/protocol/rsm'><max>1</max></set></query></iq>"
        val groupChat = Mockito.mock(GroupChat::class.java)
        `when`(groupChat.contactJid).thenReturn(ContactJid.from("group@server.domain"))
        val mamIq_groupChat = MamQueryIQ.createMamRequestIqLastMessageInChat(groupChat)
        mamIq_groupChat.stanzaId = "iqId"
        assertEquals("Error creating MAM IQ query to group chat!",
                reference_groupChat,
                mamIq_groupChat.toXML().toString(),)
    }

    fun test_createMamRequestIqMessageWithStanzaId_toXml(){
        val reference_regularChat = "<iq id='iqId' type='set'><query xmlns='urn:xmpp:mam:2'><x xmlns='jabber:x:data' type='submit'><field var='FORM_TYPE' type='hidden'><value>urn:xmpp:mam:2</value></field><field var='ids'><value>messageStanzaId</value></field></x></query></iq>"
        val regularChat = Mockito.mock(RegularChat::class.java)
        `when`(regularChat.contactJid).thenReturn(ContactJid.from("regular@server.domain"))
        val mamIq_regularChat = MamQueryIQ.createMamRequestIqMessageWithStanzaId(regularChat, "messageStanzaId")
        mamIq_regularChat.stanzaId = "iqId"
        assertEquals("Error creating with regular chat!",
                reference_regularChat,
                mamIq_regularChat.toXML().toString())

        val reference_groupChat = "<iq to='group@server.domain' id='iqId' type='set'><query xmlns='urn:xmpp:mam:2'><x xmlns='jabber:x:data' type='submit'><field var='FORM_TYPE' type='hidden'><value>urn:xmpp:mam:2</value></field><field var='ids'><value>messageStanzaId</value></field></x></query></iq>"
        val groupChat = Mockito.mock(GroupChat::class.java)
        `when`(groupChat.contactJid).thenReturn(ContactJid.from("group@server.domain"))
        val mamIq_groupChat = MamQueryIQ.createMamRequestIqMessageWithStanzaId(groupChat, "messageStanzaId")
        mamIq_groupChat.stanzaId = "iqId"
        assertEquals("Error creating MAM IQ query to group chat!",
                reference_groupChat,
                mamIq_groupChat.toXML().toString(),)
    }

    fun test_createMamRequestIqLastMessagesInChat_toXml(){
        val reference_regularChat = "<iq id='iqId' type='set'><query xmlns='urn:xmpp:mam:2'><x xmlns='jabber:x:data' type='submit'><field var='FORM_TYPE' type='hidden'><value>urn:xmpp:mam:2</value></field><field var='with'><value>regular@server.domain</value></field></x><set xmlns='http://jabber.org/protocol/rsm'><max>30</max></set></query></iq>"
        val regularChat = Mockito.mock(RegularChat::class.java)
        `when`(regularChat.contactJid).thenReturn(ContactJid.from("regular@server.domain"))
        val mamIq_regularChat = MamQueryIQ.createMamRequestIqLastMessagesInChat(regularChat, 30)
        mamIq_regularChat.stanzaId = "iqId"
        assertEquals("Error creating with regular chat!",
                reference_regularChat,
                mamIq_regularChat.toXML().toString())

        val reference_groupChat = "<iq to='group@server.domain' id='iqId' type='set'><query xmlns='urn:xmpp:mam:2'><x xmlns='jabber:x:data' type='submit'><field var='FORM_TYPE' type='hidden'><value>urn:xmpp:mam:2</value></field><field var='with'><value>group@server.domain</value></field></x><set xmlns='http://jabber.org/protocol/rsm'><max>20</max></set></query></iq>"
        val groupChat = Mockito.mock(GroupChat::class.java)
        `when`(groupChat.contactJid).thenReturn(ContactJid.from("group@server.domain"))
        val mamIq_groupChat = MamQueryIQ.createMamRequestIqLastMessagesInChat(groupChat, 20)
        mamIq_groupChat.stanzaId = "iqId"
        assertEquals("Error creating MAM IQ query to group chat!",
                reference_groupChat,
                mamIq_groupChat.toXML().toString(),)
    }

}