package com.xabber.xmpp.retract.incoming.providers

import com.xabber.android.data.TestApplication
import org.hamcrest.CoreMatchers
import org.jivesoftware.smack.packet.Message
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class IncomingReplaceExtensionElementProviderTest {

    @Test
    fun `test parse`() {
        val source =
            "<message from='juliet@capulet.lit' to='juliet@capulet.lit/phone' type='headline'>" +
                    "  <replace" +
                    "    xmlns='https://xabber.com/protocol/rewrite#notify'" +
                    "    conversation='romeo@montague.lit'" +
                    "    id='capulet321'" +
                    "    by='juliet@capulet.lit'" +
                    "    version='c1'>" +
                    "    <message from='romeo@montague.lit/phone' to='juliet@capulet.lit'>" +
                    "      <body>New body</body>" +
                    "      <stanza-id id='capulet321' by='juliet@capulet.lit' xmlns='urn:xmpp:sid:0' />" +
                    "      <replaced" +
                    "        xmlns='https://xabber.com/protocol/rewrite'" +
                    "        stamp='2018-05-11T03:49:00' />" +
                    "    </message>" +
                    "  </replace>" +
                    "</message>"

        val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
            setInput(StringReader(source))
        }

        val element = IncomingReplaceExtensionElementProvider().parse(parser, 0)

        Assert.assertEquals("Failed to parse message stanza id", "capulet321", element.messageStanzaId)
        Assert.assertEquals("Failed to parse contact jid", "juliet@capulet.lit", element.contactJid.toString())
        Assert.assertEquals("Failed to parse version", "c1", element.version)

        Assert.assertThat(
            "Inner message was parsed incorrectly",
            element.message,
            CoreMatchers.instanceOf(Message::class.java)
        )

        Assert.assertEquals("Failed to parse body", "New body", element.message.body)
    }
}