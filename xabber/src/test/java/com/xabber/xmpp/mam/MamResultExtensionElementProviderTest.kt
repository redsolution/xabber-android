package com.xabber.xmpp.mam

import com.xabber.android.data.TestApplication
import junit.framework.TestCase
import org.hamcrest.CoreMatchers.instanceOf
import org.jivesoftware.smack.packet.Message
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class MamResultExtensionElementProviderTest : TestCase() {

    @Test
    fun test_parse_without_queryid() {
        val source =
                "<result id='resultId' xmlns='urn:xmpp:mam:2'>\n" +
                        "        <forwarded xmlns='urn:xmpp:forward:0'>\n" +
                        "            <message xml:lang='en' to='account@server.com' from='contact@other.do/some_resource' type='chat' id='chatId'xmlns='jabber:client'>\n" +
                        "                <body>message text body</body>\n" +
                        "            </message>\n" +
                        "         </forwarded>\n" +
                        "    </result>"

        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(source))

        val mamResultElement = MamResultExtensionElementProvider().parse(parser, 0)

        assertEquals("Failed to parse id", "resultId", mamResultElement.id)
        assertNotNull("Failed to parse forwarded", mamResultElement.forwarded)
        assertNull("Failed to parse queryId", mamResultElement.queryId)
        assertThat("Forwarded was parsed incorrectly",
                mamResultElement.forwarded.forwardedStanza, instanceOf(Message::class.java))
    }

    @Test
    fun test_parse_with_queryid(){
        val sourceWithQueryId =
                "<result id='resultId' xmlns='urn:xmpp:mam:2' queryid='queryID1'>\n" +
                        "        <forwarded xmlns='urn:xmpp:forward:0'>\n" +
                        "            <message xml:lang='en' to='account@server.com' from='contact@other.do/some_resource' type='chat' id='chatId'xmlns='jabber:client'>\n" +
                        "                <body>message text body</body>\n" +
                        "            </message>\n" +
                        "         </forwarded>\n" +
                        "    </result>"


        val parserWithQueryId = XmlPullParserFactory.newInstance().newPullParser()
        parserWithQueryId.setInput(StringReader(sourceWithQueryId))

        val mamResultElementWithQueryId = MamResultExtensionElementProvider().parse(parserWithQueryId, 0)

        assertEquals("Failed to parse id", "resultId", mamResultElementWithQueryId.id)
        assertNotNull("Failed to parse forwarded", mamResultElementWithQueryId.forwarded)
        assertNotNull("Failed to parse queryId", mamResultElementWithQueryId.queryId)
        assertEquals("Incorrect parsing query id", "queryID1", mamResultElementWithQueryId.queryId)
        assertThat("Forwarded was parsed incorrectly",
                mamResultElementWithQueryId.forwarded.forwardedStanza, instanceOf(Message::class.java))
    }

}