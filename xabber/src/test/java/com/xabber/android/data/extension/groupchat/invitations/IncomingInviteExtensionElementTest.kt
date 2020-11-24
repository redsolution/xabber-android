package com.xabber.android.data.extension.groupchat.invitations

import com.xabber.android.data.extension.groupchat.invite.incoming.IncomingInviteExtensionElement
import org.junit.Assert
import org.junit.Test

class IncomingInviteExtensionElementTest {

    @Test
    fun test_toXML_without_child_elements(){
        val inviteElement = IncomingInviteExtensionElement().apply {
            groupJid = "group@jid.com"
        }
        val reference = "<invite jid='group@jid.com' xml:lang='https://xabber.com/protocol/groups#invite'></invite>"
        Assert.assertEquals(reference, inviteElement.toXML().toString())
    }

    @Test
    fun test_toXML_with_child_elements(){
        val inviteElement = IncomingInviteExtensionElement().apply {
            groupJid = "group@jid.com"
            setReason("Reason")
            setUser("sender@jid.org", "memberId")
        }
        val reference = "<invite jid='group@jid.com' xml:lang='https://xabber.com/protocol/groups#invite'><reason>Reason</reason><user jid='sender@jid.org' id='memberId'/></invite>"
        Assert.assertEquals(reference, inviteElement.toXML().toString())
    }

}