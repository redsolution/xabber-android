package com.xabber.android.data.extension.groupchat

import com.xabber.android.data.extension.android.groupchat.rights.GroupchatMemberRightsQueryIQ
import org.junit.Assert
import org.junit.Test
import org.jxmpp.jid.impl.JidCreate

class GroupMemberRightsQueryIQTest {

    @Test
    fun testGroupchatMemberRightsQueryIqBuilding() {

        val reference = "<iq to='groupchat@server.org/resource' id='48ZxA-1' type='get'><query xmlns='https://xabber.com/protocol/groups#rights'><user xmlns='https://xabber.com/protocol/groups' id='userId'></user></query></iq>"

        val groupchatJid = JidCreate.fullFrom("groupchat@server.org/resource")
        val iq = GroupchatMemberRightsQueryIQ(groupchatJid, "userId").apply { stanzaId = "48ZxA-1" }

        Assert.assertEquals(reference, iq.toXML().toString())
    }

}