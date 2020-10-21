package com.xabber.android.data.extension.groupchat

import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.groupchat.rights.GroupchatMemberRightsQueryIQ
import org.junit.Assert
import org.junit.Test

class GroupchatMemberRightsQueryIQTest {

    @Test
    fun testGroupchatMemberRightsQueryIqBuilding() {

        val reference = "<iq to='groupchat@server.org' id='48ZxA-1' type='get'><query xmlns='http://xabber.com/protocol/groupchat#rights'><user xmlns='http://xabber.com/protocol/groupchat' id='userId'></user></query></iq>"

        val groupchatJid = ContactJid.from("groupchat@server.org")
        val iq = GroupchatMemberRightsQueryIQ(groupchatJid, "userId").apply { stanzaId = "48ZxA-1" }

        Assert.assertEquals(reference, iq.toXML().toString())
    }

    @Test
    fun testChangeMemberRightsRequest(){
//        val reference = ""
//
//       val iq = GroupRequestMemberRightsChangeIQ()
    }

}