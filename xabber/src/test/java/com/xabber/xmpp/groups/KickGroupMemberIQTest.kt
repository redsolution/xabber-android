package com.xabber.xmpp.groups

import com.xabber.xmpp.groups.block.KickGroupMemberIQ
import junit.framework.TestCase
import org.jxmpp.jid.impl.JidCreate

class KickGroupMemberIQTest : TestCase() {

    fun test_getIQChildElementBuilder() {

        val groupFullJid = JidCreate.fullFrom("group@group-server.domain/resource")

        val memberJid = JidCreate.from("member@member-server.domain")
        val iqWithJid = KickGroupMemberIQ(memberJid, groupFullJid)
        iqWithJid.stanzaId = "iqId"
        val referenceWithJid =
                "<iq to='group@group-server.domain/resource' id='iqId' type='set'>" +
                    "<kick xmlns='https://xabber.com/protocol/groups'>" +
                        "<jid>member@member-server.domain</jid>" +
                    "</kick>" +
                "</iq>"
        assertEquals(referenceWithJid, iqWithJid.toXML().toString())

        val memberId = "memberId"
        val iqWithId = KickGroupMemberIQ(memberId, groupFullJid)
        iqWithId.stanzaId = "iqId"
        val referenceWithId =
                "<iq to='group@group-server.domain/resource' id='iqId' type='set'>" +
                    "<kick xmlns='https://xabber.com/protocol/groups'>" +
                        "<id>memberId</id>" +
                    "</kick>" +
                "</iq>"
        assertEquals(referenceWithId, iqWithId.toXML().toString())

        try {
            val iqWithEmptyId = KickGroupMemberIQ("", groupFullJid)
            iqWithEmptyId.toXML()
            fail("Expected exception was not occured")
        } catch (e: Exception) {
        }
    }

}