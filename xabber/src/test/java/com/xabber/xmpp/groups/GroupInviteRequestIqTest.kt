package com.xabber.xmpp.groups

import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.xmpp.groups.invite.outgoing.GroupInviteRequestIQ
import org.junit.Assert.assertEquals
import org.junit.Test
import org.jxmpp.jid.impl.JidCreate
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class GroupInviteRequestIqTest {

    private fun getBaseIq(): GroupInviteRequestIQ {
        val group = mock(GroupChat::class.java)
        val groupFullJid = JidCreate.fullFrom("localpart@group.domain/resource")
        val whomInviteJid = ContactJid.from("memberToInvite@server.domain")

        `when`(group.fullJidIfPossible).thenReturn(groupFullJid)

        val iq = GroupInviteRequestIQ(group, whomInviteJid)
        iq.stanzaId = "iqId"

        return iq
    }

    @Test
    fun test_getNamespace(){
        assertEquals("https://xabber.com/protocol/groups#invite", getBaseIq().childElementNamespace)
    }

    @Test
    fun test_getElementName(){
        assertEquals("invite", getBaseIq().childElementName)
    }

    @Test
    fun test_toXml(){
        val reference1 =
                "<iq to='localpart@group.domain/resource' id='iqId' type='set'>" +
                    "<invite xmlns='https://xabber.com/protocol/groups#invite'>" +
                        "<jid>membertoinvite@server.domain</jid>" +
                        "<send>false</send>" +
                    "</invite>" +
                "</iq>"
        assertEquals(reference1, getBaseIq().toXML().toString())

        val reference2 =
                "<iq to='localpart@group.domain/resource' id='iqId' type='set'>" +
                    "<invite xmlns='https://xabber.com/protocol/groups#invite'>" +
                        "<jid>membertoinvite@server.domain</jid>" +
                        "<send>true</send>" +
                    "</invite>" +
                "</iq>"
        assertEquals(reference2, getBaseIq().apply { setLetGroupSendInviteMessage(true) }.toXML().toString())

        val reference3 =
                "<iq to='localpart@group.domain/resource' id='iqId' type='set'>" +
                    "<invite xmlns='https://xabber.com/protocol/groups#invite'>" +
                        "<jid>membertoinvite@server.domain</jid>" +
                        "<send>false</send>" +
                    "</invite>" +
                "</iq>"
        assertEquals(reference3, getBaseIq().apply { setLetGroupSendInviteMessage(false) }.toXML().toString())

        val reference4 =
                "<iq to='localpart@group.domain/resource' id='iqId' type='set'>" +
                    "<invite xmlns='https://xabber.com/protocol/groups#invite'>" +
                        "<jid>membertoinvite@server.domain</jid>" +
                        "<send>false</send>" +
                    "</invite>" +
                "</iq>"
        assertEquals(reference4, getBaseIq().toXML().toString())

        val reference5 =
                "<iq to='localpart@group.domain/resource' id='iqId' type='set'>" +
                    "<invite xmlns='https://xabber.com/protocol/groups#invite'>" +
                        "<jid>membertoinvite@server.domain</jid>" +
                        "<send>true</send>" +
                    "</invite>" +
                "</iq>"
        assertEquals(reference5, getBaseIq().apply {
            setLetGroupSendInviteMessage(true)
        }.toXML().toString())

    }

}