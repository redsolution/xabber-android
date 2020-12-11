package com.xabber.android.data.extension.groupchat

import com.xabber.android.data.extension.groupchat.settings.GroupSettingsRequestFormQueryIQ
import org.junit.Assert.assertEquals
import org.junit.Test
import org.jxmpp.jid.impl.JidCreate

class GroupSettingsTest{

    private val groupchatJid = JidCreate.fullFrom("group@server.domain/resource")

    @Test
    fun testFormRequestIqCreating(){
        val iq = GroupSettingsRequestFormQueryIQ(groupchatJid)
        val expected = """<iq to='group@server.domain/resource' id='${iq.stanzaId}' type='get'><query xmlns='https://xabber.com/protocol/groups'></query></iq>""".trimIndent()
        assertEquals(expected, iq.toXML().toString())
    }

}