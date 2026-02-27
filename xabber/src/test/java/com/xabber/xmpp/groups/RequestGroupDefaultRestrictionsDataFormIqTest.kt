package com.xabber.xmpp.groups

import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.xmpp.groups.restrictions.RequestGroupDefaultRestrictionsDataFormIQ
import org.junit.Assert
import org.junit.Test

class RequestGroupDefaultRestrictionsDataFormIqTest {

//    @Test
//    fun test_getIQChildElementBuilder(){
//        val groupchat = GroupChat(AccountJid.from("account@server.do/resource"), ContactJid.from("contect@jid.com"))
//
//        val iqId = "iqId"
//
//        val iq = RequestGroupDefaultRestrictionsDataFormIQ(groupchat).apply {
//            stanzaId = iqId
//        }
//        val reference = "<iq to='contect@jid.com' id='$iqId' type='get'><query xmlns='https://xabber" +
//                ".com/protocol/groups#default-rights'/></iq>"
//
//        Assert.assertEquals(reference, iq.toXML().toString())
//    }

}