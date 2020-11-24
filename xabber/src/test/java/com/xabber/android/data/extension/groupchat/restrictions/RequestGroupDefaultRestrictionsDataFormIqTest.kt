package com.xabber.android.data.extension.groupchat.restrictions

import com.xabber.android.data.entity.ContactJid
import org.junit.Assert
import org.junit.Test

class RequestGroupDefaultRestrictionsDataFormIqTest {

    @Test
    fun test_getIQChildElementBuilder(){
        var contactJid: ContactJid? = null

        try{
            contactJid = ContactJid.from("contect@jid.com")
        } catch (e: Exception) { e.printStackTrace() }

        val iqId = "iqId"

        val iq = RequestGroupDefaultRestrictionsDataFormIQ(contactJid!!).apply {
            stanzaId = iqId
        }
        val reference = "<iq to='contect@jid.com' id='$iqId' type='get'><query xmlns='https://xabber" +
                ".com/protocol/groups#default-rights'/></iq>"

        Assert.assertEquals(reference, iq.toXML().toString())
    }

}