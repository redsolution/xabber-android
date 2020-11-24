package com.xabber.android.data.extension.groupchat

import com.xabber.android.data.TestApplication
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.groupchat.members.ChangeGroupchatMemberPreferencesIQ
import org.junit.Assert
import org.junit.Test
import org.robolectric.annotation.Config

@Config(sdk = [28], application = TestApplication::class)
class ChangeGroupchatMemberPreferencesIqTest {

    @Test
    fun testIqCreatingSetBadge() {
        val reference = "<iq to='groupchat@otherdomain.com' id='iqId' type='set'><query xmlns='https://xabber.com/protocol/groups#members'><user id='memberId' xmlns='https://xabber.com/protocol/groups'><badge>badge!</badge></user></query></iq>"
        val groupchatJid = ContactJid.from("groupchat@otherdomain.com")

        val memberId = "memberId"
        val badge = "badge!"
        val iqId = "iqId"

        val iq = ChangeGroupchatMemberPreferencesIQ(groupchatJid,
                memberId, badge, null).apply { stanzaId = iqId }

        Assert.assertEquals(reference, iq.toXML().toString())
    }

    @Test
    fun testIqCreatingSetNickname() {
        val reference = "<iq to='groupchat@otherdomain.com' id='iqId' type='set'><query xmlns='https://xabber.com/protocol/groups#members'><user id='memberId' xmlns='https://xabber.com/protocol/groups'><nickname>nickname!</nickname></user></query></iq>"
        val groupchatJid = ContactJid.from("groupchat@otherdomain.com")

        val memberId = "memberId"
        val nickname = "nickname!"
        val iqId = "iqId"

        val iq = ChangeGroupchatMemberPreferencesIQ(groupchatJid,
                memberId, null, nickname).apply { stanzaId = iqId }

        Assert.assertEquals(reference, iq.toXML().toString())
    }

}