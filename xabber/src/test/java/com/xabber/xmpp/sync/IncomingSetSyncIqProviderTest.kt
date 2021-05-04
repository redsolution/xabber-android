package com.xabber.xmpp.sync

import com.xabber.android.data.TestApplication
import com.xabber.android.data.extension.sync.SyncManager
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class IncomingSetSyncIqProviderTest {

    @Test
    fun parse_test(){
        val source = "<iq to='account@server.do/resource' from='server@server.domain' id='stanzaId' type='set'>\" +\n" +
                "                    \"<query xmlns='https://xabber.com/protocol/synchronization' stamp='1000000'>\" +\n" +
                "                        \"<conversation jid='jidofconversation@server.do'>\" +\n" +
                "                            \"<deleted/>\" +\n" +
                "                        \"</conversation>\" +\n" +
                "                    \"</query>\" +\n" +
                "                \"</iq>"


        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(source))

        val iq = IncomingSetSyncIqProvider().parse(parser, 0)

        //assertEquals("account@server.do/resource", iq.to.toString())
        //assertEquals("server@server.domain", iq.from.toString())
        assertEquals(SyncManager.NAMESPACE, iq.childElementNamespace)
        assertEquals(IncomingSetSyncIQ.QUERY_ELEMENT, iq.childElementName)
        assertEquals("1000000".toLong(), iq.stamp)
        assertEquals(ConversationExtensionElement.ELEMENT_NAME, iq.extensionElement.elementName)

        assertEquals("jidofconversation@server.do",
                (iq.extensionElement as ConversationExtensionElement).jid.toString())

        assertEquals(DeletedElement.ELEMENT_NAME,
                (iq.extensionElement as ConversationExtensionElement).childElement.elementName)
    }
}