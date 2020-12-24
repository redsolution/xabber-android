package com.xabber.android.data.extension.groupchat.invite.incoming

import com.xabber.android.data.extension.groupchat.invite.incoming.DeclineGroupInviteIQ
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.jxmpp.jid.impl.JidCreate

class DeclineGroupInviteTest {

    private val referenceGroupJid = "group@domain.org/resource"
    private val referenceIqId = "iqId"
    private val referenceIqXml = "<iq to='group@domain.org/resource' id='iqId' type='set'><decline xmlns='https://xabber.com/protocol/groups#invite'></decline></iq>"

    private lateinit var iq: DeclineGroupInviteIQ

    @Before
    fun setup(){
        val contactJid = JidCreate.fullFrom(referenceGroupJid)
        iq = DeclineGroupInviteIQ(contactJid)
        iq.stanzaId = referenceIqId
    }

    @Test
    fun test_getIQChildElementBuilder(){
        Assert.assertEquals(iq.toXML().toString(), referenceIqXml)
    }

}