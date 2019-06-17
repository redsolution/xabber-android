package com.xabber.android.data.extension.references;

import com.xabber.android.data.TestApplication;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(application = TestApplication.class)
public class ReferencesProviderTest {

    private ReferencesProvider provider;
    private XmlPullParserFactory factory;
    private String stringForward, stringMedia, stringMarkup1,
            stringMarkup2, stringMention, stringQuote, stringNull, stringUnknown;

    @Before
    public void setUp() throws Exception {
        provider = new ReferencesProvider();
        factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);

        stringForward = "<reference xmlns=\"urn:xmpp:reference:0\" type=\"forward\" begin=\"11\" end=\"179\">" +
                "<forwarded xmlns=\"urn:xmpp:forward:0\">" +
                "<delay xmlns=\"urn:xmpp:delay\" stamp=\"2010-07-10T23:08:25Z\"/>" +
                "<message from=\"valery\" to=\"xabber\" type=\"chat\" id=\"97\">" +
                "<body>hello</body>" +
                "</message>" +
                "</forwarded>" +
                "</reference>";

        stringMedia = "<reference xmlns='urn:xmpp:reference:0' type='data' begin=\"0\" end=\"89\"><media><file><media-type>" +
                "application/pdf</media-type><name>Android-Architecture_1-1.pdf</name><size>4255465" +
                "</size></file><uri>https://upload02.xabber.org/5f70e738285c44c82039a73d42eccf27" +
                "44e91/lQk6DkRJ/Android-Architecture_1-1.pdf</uri></media><media><file><media-type>" +
                "image/png</media-type><name>Screenshot_20190414-194652.png</name><size>251184</size>" +
                "<height>1280</height><width>720</width></file><uri>https://upload02.xabber.org/5f70" +
                "e738285c44c82039a73d42eccf2744e91/rUdy3rHt/Screenshot_20190414-194652.png</uri>" +
                "</media><bold/><italic/></reference>";

        stringMarkup1 = "<reference xmlns='urn:xmpp:reference:0' begin='7' end='10'  type='markup'>" +
                        "<bold/><italic/></reference>";

        stringMarkup2 = "<reference xmlns='urn:xmpp:reference:0' begin='34' end='37'  type='markup'>" +
                        "<bold/><url>https://www.xabber.com</url></reference>";

        stringMention = "<reference xmlns='urn:xmpp:reference:0' begin='16' end='22' type='mention' " +
                        "uri='xmpp:juliet@capulet.lit'/>";

        stringQuote = "<reference xmlns='urn:xmpp:reference:0' begin='0' end='31' del='5' type='quote'/>";

        stringUnknown = "<reference xmlns='urn:xmpp:reference:0' end='17' begin='0' type='unknown'></reference>";
        stringNull = "<reference xmlns='urn:xmpp:reference:0' end='17' begin='0'></reference>";

    }

    @Test
    public void parse1() {
        Forward element = (Forward) parseString(stringForward);
        assertNotNull(element);
        assertEquals("forward", element.getType().toString());
        assertEquals(11, element.getBegin());
        assertEquals(179, element.getEnd());
        assertEquals(1, element.getForwarded().size());
    }

    @Test
    public void parse2() {
        Data element = (Data) parseString(stringMedia);
        assertNotNull(element);
        assertEquals("data", element.getType().toString());
        assertEquals(0, element.getBegin());
        assertEquals(89, element.getEnd());
        assertEquals(2, element.getMedia().size());

        RefMedia media1 = element.getMedia().get(0);
        assertNotNull(media1);
        assertEquals("https://upload02.xabber.org/5f70e738285c44c82039a73" +
                "d42eccf2744e91/lQk6DkRJ/Android-Architecture_1-1.pdf", media1.getUri());

        RefFile file1 = media1.getFile();
        assertNotNull(file1);
        assertEquals("application/pdf", file1.getMediaType());
        assertEquals("Android-Architecture_1-1.pdf", file1.getName());
        assertEquals(4255465, file1.getSize());

        RefMedia media2 = element.getMedia().get(1);
        assertNotNull(media2);
        assertEquals("https://upload02.xabber.org/5f70e738285c44c82039a73d42eccf274" +
                "4e91/rUdy3rHt/Screenshot_20190414-194652.png", media2.getUri());

        RefFile file2 = media2.getFile();
        assertNotNull(file2);
        assertEquals("image/png", file2.getMediaType());
        assertEquals("Screenshot_20190414-194652.png", file2.getName());
        assertEquals(251184, file2.getSize());
        assertEquals(1280, file2.getHeight());
        assertEquals(720, file2.getWidth());
    }

    @Test
    public void parse3() {
        Markup element = (Markup) parseString(stringMarkup1);
        assertNotNull(element);
        assertEquals("markup", element.getType().toString());
        assertEquals(7, element.getBegin());
        assertEquals(10, element.getEnd());
        assertTrue(element.isBold());
        assertTrue(element.isItalic());
        assertFalse(element.isStrike());
        assertFalse(element.isUnderline());
        assertNull(element.getUrl());
    }

    @Test
    public void parse4() {
        Markup element = (Markup) parseString(stringMarkup2);
        assertNotNull(element);
        assertEquals("markup", element.getType().toString());
        assertEquals(34, element.getBegin());
        assertEquals(37, element.getEnd());
        assertTrue(element.isBold());
        assertFalse(element.isItalic());
        assertFalse(element.isStrike());
        assertFalse(element.isUnderline());
        assertEquals("https://www.xabber.com", element.getUrl());
    }

    @Test
    public void parse5() {
        Mention element = (Mention) parseString(stringMention);
        assertNotNull(element);
        assertEquals("mention", element.getType().toString());
        assertEquals(16, element.getBegin());
        assertEquals(22, element.getEnd());
        assertEquals("xmpp:juliet@capulet.lit", element.getUri());
    }

    @Test
    public void parse6() {
        Quote element = (Quote) parseString(stringQuote);
        assertNotNull(element);
        assertEquals("quote", element.getType().toString());
        assertEquals(0, element.getBegin());
        assertEquals(31, element.getEnd());
        assertEquals(5, element.getDel());
    }

    @Test
    public void parse7() {
        ReferenceElement element = parseString(stringUnknown);
        assertNull(element);

        ReferenceElement element1 = parseString(stringNull);
        assertNull(element1);
    }

    private ReferenceElement parseString(String source) {
        ReferenceElement result = null;
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