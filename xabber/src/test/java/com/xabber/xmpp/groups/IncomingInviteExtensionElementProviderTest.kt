package com.xabber.xmpp.groups

import com.xabber.android.data.TestApplication
import com.xabber.xmpp.groups.invite.incoming.IncomingInviteExtensionElementProvider
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class IncomingInviteExtensionElementProviderTest {

    @Test
    fun test_parse_with_reason_and_user_element(){
        val reason = "This is reason"
        val senderJid = "sender@jid.com"
        val memberId = "memberId"
        val groupJid = "group@jid.com"

        val xml = "<invite xmlns=\"https://xabber.com/protocol/groups#invite\" jid=\"$groupJid\">" +
                "    <reason>$reason</reason>\n" +
                "    <user jid=\"$senderJid\" id=\"$memberId\" />\n" +
                "</invite>"
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))

        val invite = IncomingInviteExtensionElementProvider().parse(parser, 0)

        Assert.assertEquals(reason, invite.getReason())
        Assert.assertEquals(senderJid, invite.getUserJid())
        Assert.assertEquals(memberId, invite.getUserId())
        Assert.assertEquals(groupJid, invite.groupJid)
    }

    @Test
    fun test_parse_without_child_elements(){
        val groupJid = "group@jid.com"

        val xml = "<invite xmlns=\"https://xabber.com/protocol/groups#invite\" jid=\"$groupJid\">" +
                "</invite>"

        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))

        val invite = IncomingInviteExtensionElementProvider().parse(parser, 0)

        Assert.assertEquals("", invite.getReason())
        Assert.assertEquals("", invite.getUserJid())
        Assert.assertEquals("", invite.getUserId())
        Assert.assertEquals(groupJid, invite.groupJid)
    }

    @Test
    fun test_parse_with_all_empty(){
        val xml = "<invite xmlns=\"https://xabber.com/protocol/groups#invite\">" +
                "</invite>"

        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))

        val invite = IncomingInviteExtensionElementProvider().parse(parser, 0)

        Assert.assertEquals("", invite.getReason())
        Assert.assertEquals("", invite.getUserJid())
        Assert.assertEquals("", invite.getUserId())
        Assert.assertEquals("", invite.groupJid)
    }

}