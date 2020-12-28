package com.xabber.android.data.extension.groupchat.block

import junit.framework.TestCase
import org.jxmpp.jid.impl.JidCreate

class BlockGroupMemberIQTest : TestCase() {

    fun test_getIQChildElementBuilder() {

        val groupFullJid = JidCreate.fullFrom("group@group-server.domain/resource")

        val memberJid = JidCreate.from("member@member-server.domain")
        val iqWithJid = BlockGroupMemberIQ(groupFullJid, memberJid)
        iqWithJid.stanzaId = "iqId"
        val referenceWithJid =
                "<iq to='group@group-server.domain/resource' id='iqId' type='set'>" +
                    "<block xmlns='https://xabber.com/protocol/groups#block'>" +
                        "<jid>member@member-server.domain</jid>" +
                    "</block>" +
                "</iq>"
        assertEquals(referenceWithJid, iqWithJid.toXML().toString())

        val memberId = "memberId"
        val iqWithId = BlockGroupMemberIQ(groupFullJid, memberId)
        iqWithId.stanzaId = "iqId"
        val referenceWithId =
                "<iq to='group@group-server.domain/resource' id='iqId' type='set'>" +
                    "<block xmlns='https://xabber.com/protocol/groups#block'>" +
                        "<id>memberId</id>" +
                    "</block>" +
                "</iq>"
        assertEquals(referenceWithId, iqWithId.toXML().toString())

        try {
            val iqWithEmptyId = BlockGroupMemberIQ(groupFullJid, "")
            iqWithEmptyId.toXML()
            fail("Expected exception was not occured")
        } catch (e: Exception) {
        }

    }

}