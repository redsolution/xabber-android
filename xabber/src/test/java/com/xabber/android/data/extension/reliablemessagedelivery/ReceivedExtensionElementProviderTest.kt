package com.xabber.android.data.extension.reliablemessagedelivery

import com.xabber.android.data.TestApplication
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class ReceivedExtensionElementProviderTest {

    @Test
    fun testParse(){
        val referenceTimestamp = "2020-03-16T11:46:33.970745Z"
        val referenceStanzaId = "1584359193970745"
        val referenceOriginId = "fa20384a-75ea-4d4e-bb39-49e0fd55473b"

        val sourceXml = "<received xmlns=\"https://xabber.com/protocol/delivery\">\n" +
                "    <time by=\"juliet@capuliet.it\" stamp=\"2020-03-16T11:46:33.970745Z\" />\n" +
                "    <origin-id id=\"fa20384a-75ea-4d4e-bb39-49e0fd55473b\" xmlns=\"urn:xmpp:sid:0\" />\n" +
                "    <stanza-id by=\"juliet@capuliet.it\" id=\"1584359193970745\" xmlns=\"urn:xmpp:sid:0\" />\n" +
                "</received>"

        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(sourceXml))

        val receivedElement = ReceivedExtensionElementProvider().parse(parser, 0)

        Assert.assertEquals(referenceOriginId, receivedElement.originIdElement?.id)
        Assert.assertEquals(referenceStanzaId, receivedElement.stanzaIdElement?.id)
        Assert.assertEquals(referenceTimestamp, receivedElement.timeElement?.timeStamp)
    }

}