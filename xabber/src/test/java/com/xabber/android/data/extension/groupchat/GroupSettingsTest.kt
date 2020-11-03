package com.xabber.android.data.extension.groupchat


import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.groupchat.settings.GroupSettingsRequestFormQueryIQ
import org.junit.Assert.assertEquals
import org.junit.Test


class GroupSettingsTest{

    private val groupchatJid = ContactJid.from("group@server.domain")

    @Test
    fun testFormRequestIqCreating(){
        val iq = GroupSettingsRequestFormQueryIQ(groupchatJid)
        val expected = """<iq to='group@server.domain' id='${iq.stanzaId}' type='get'><query xmlns='https://xabber.com/protocol/groups'></query></iq>""".trimIndent()
        assertEquals(expected, iq.toXML().toString())
    }

}