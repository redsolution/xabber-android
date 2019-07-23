package com.xabber.android.data.extension.xtoken;

import com.xabber.android.data.TestApplication;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = TestApplication.class)
public class SessionsProviderTest {

    private SessionsProvider provider;
    private XmlPullParserFactory factory;
    private String stanza;

    @Before
    public void setUp() throws Exception {
        provider = new SessionsProvider();
        factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);

        stanza = "<x xmlns='http://xabber.com/protocol/auth-tokens#items'>" +
                "<field var='1'>" +
                "<client>xabber-web 2.3</client>" +
                "<device>iMac Pro MacOS 10.14</device>" +
                "<token-uid>024717297867c1d32714cadde305825a9909ef7c</token-uid>" +
                "<expire>1636322632</expire>" +
                "<ip>127.0.0.1</ip>" +
                "<last-auth>1536322632</last-auth>" +
                "</field>" +
                "<field var='2'>" +
                "<client>xabber-android 2.363</client>" +
                "<device>Nokia  Android 8.0</device>" +
                "<token-uid>7dbf8541c4de1d24a0f748cc01f98a140100979a</token-uid>" +
                "<expire>1736322632</expire>" +
                "<ip>127.0.1.1</ip>" +
                "<last-auth>1436322632</last-auth>" +
                "</field>" +
                "</x>";
    }

    @Test
    public void parse() {
        SessionsIQ element = parseString(stanza);
        assertNotNull(element);
        assertEquals(2, element.getSessions().size());

        Session session1 = element.getSessions().get(0);
        assertNotNull(session1);
        assertEquals("xabber-web 2.3", session1.getClient());
        assertEquals("iMac Pro MacOS 10.14", session1.getDevice());
        assertEquals("024717297867c1d32714cadde305825a9909ef7c", session1.getUid());
        assertEquals("127.0.0.1", session1.getIp());
        assertEquals(1636322632000L, session1.getExpire());
        assertEquals(1536322632000L, session1.getLastAuth());

        Session session2 = element.getSessions().get(1);
        assertNotNull(session2);
        assertEquals("xabber-android 2.363", session2.getClient());
        assertEquals("Nokia  Android 8.0", session2.getDevice());
        assertEquals("7dbf8541c4de1d24a0f748cc01f98a140100979a", session2.getUid());
        assertEquals("127.0.1.1", session2.getIp());
        assertEquals(1736322632000L, session2.getExpire());
        assertEquals(1436322632000L, session2.getLastAuth());

    }

    private SessionsIQ parseString(String source) {
        SessionsIQ result = null;
        try {
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(source));
            result = provider.parse(parser, 0);
        } catch (Exception e) {
            fail("Exception while parsing: " + e.toString());
        }
        return result;
    }
}