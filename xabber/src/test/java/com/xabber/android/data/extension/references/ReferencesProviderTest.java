package com.xabber.android.data.extension.references;

import com.xabber.android.data.TestApplication;
import com.xabber.android.data.extension.groupchat.Groupchat;
import com.xabber.android.data.extension.groupchat.GroupchatPresence;
import com.xabber.android.data.extension.groupchat.GroupchatProvider;
import com.xabber.android.data.extension.groupchat.GroupchatUserContainer;
import com.xabber.android.data.extension.groupchat.GroupchatUserExtension;
import com.xabber.android.data.extension.references.decoration.Markup;
import com.xabber.android.data.extension.references.mutable.Forward;

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
@Config(sdk = 28, application = TestApplication.class)
public class ReferencesProviderTest {

    private ReferencesProvider provider;
    private GroupchatProvider groupchatProvider;
    private XmlPullParserFactory factory;
    private String stringForward, stringMedia, stringMarkup1,
            stringMarkup2, stringMention, stringQuote, stringNull, stringUnknown, stringGroupchat, stringGroupPresence;

    @Before
    public void setUp() throws Exception {
        provider = new ReferencesProvider();
        groupchatProvider = new GroupchatProvider();
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

        stringMedia = "<reference xmlns='urn:xmpp:reference:0' type='media' begin=\"0\" end=\"89\"><media><file><media-type>" +
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
                        "<bold/><uri>https://www.xabber.com</uri></reference>";

        stringMention = "<reference xmlns='urn:xmpp:reference:0' begin='16' end='22' type='mention'>" +
                        "<uri>xmpp:juliet@capulet.lit</uri></reference>";

        stringQuote = "<reference xmlns='urn:xmpp:reference:0' begin='0' end='31' type='quote'>" +
                      "<marker>&gt; </marker></reference>";

        stringUnknown = "<reference xmlns='urn:xmpp:reference:0' end='17' begin='0' type='unknown'></reference>";
        stringNull = "<reference xmlns='https://xabber.com/protocol/reference' end='17' begin='0'></reference>";

        stringGroupchat = "<x xmlns='http://xabber.com/protocol/groupchat'>" +
                "<reference xmlns='urn:xmpp:reference:0' end='16' begin='0' type='groupchat'>" +
                "<user xmlns='http://xabber.com/protocol/groupchat' id='kubsgzldk3csvtez'>" +
                "<role>member</role>" +
                "<nickname>john.doe</nickname>" +
                "<badge />" +
                "<jid>john.doe@xabber.org</jid>" +
                "<metadata xmlns='urn:xmpp:avatar:metadata'>" +
                "<info url='http://xabber.org/images/d7072b2bc4652580911649870699787b18.jpeg' " +
                "type='image/jpeg' id='d7072b2bc4652580911649870699787b18' bytes='9145' />" +
                "</metadata>'" +
                "</user>'" +
                "</reference>" +
                "</x>";

        stringGroupPresence = "<x xmlns='http://xabber.com/protocol/groupchat'>" +
                "<name>My Chat</name>" +
                "<privacy>public</privacy>" +
                "<pinned-message>1464523434</pinned-message>" +
                "<collect>no</collect>" +
                "<peer-to-peer>true</peer-to-peer>" +
                "<status>chat</status>" +
                "<present>78</present>" +
                "<members>225</members>" +
                "</x>";
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

/*
    @Test
    public void parse2() {
        Media element = (Media) parseString(stringMedia);
        assertNotNull(element);
        assertEquals("media", element.getType().toString());
        assertEquals(0, element.getBegin());
        assertEquals(89, element.getEnd());
        assertEquals(2, element.getMedia().size());

        RefMedia media1 = element.getMedia().get(0);
        assertNotNull(media1);
        assertEquals("https://upload02.xabber.org/5f70e738285c44c82039a73" +
                "d42eccf2744e91/lQk6DkRJ/Android-Architecture_1-1.pdf", media1.getUri());

        FileSharingExtension file1 = media1.getFile();
        assertNotNull(file1);
        assertEquals("application/pdf", file1.getMediaType());
        assertEquals("Android-Architecture_1-1.pdf", file1.getName());
        assertEquals(4255465, file1.getSize());

        RefMedia media2 = element.getMedia().get(1);
        assertNotNull(media2);
        assertEquals("https://upload02.xabber.org/5f70e738285c44c82039a73d42eccf274" +
                "4e91/rUdy3rHt/Screenshot_20190414-194652.png", media2.getUri());

        FileSharingExtension file2 = media2.getFile();
        assertNotNull(file2);
        assertEquals("image/png", file2.getMediaType());
        assertEquals("Screenshot_20190414-194652.png", file2.getName());
        assertEquals(251184, file2.getSize());
        assertEquals(1280, file2.getHeight());
        assertEquals(720, file2.getWidth());
    }
*/

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
        assertNull(element.getLink());
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
        assertEquals("https://www.xabber.com", element.getLink());
    }

/*
    @Test
    public void parse5() {
        Mention element = (Mention) parseString(stringMention);
        assertNotNull(element);
        assertEquals("mention", element.getType().toString());
        assertEquals(16, element.getBegin());
        assertEquals(22, element.getEnd());
        assertEquals("xmpp:juliet@capulet.lit", element.getUri());
    }
*/

/*
    @Test
    public void parse6() {
        Quote element = (Quote) parseString(stringQuote);
        assertNotNull(element);
        assertEquals("quote", element.getType().toString());
        assertEquals(0, element.getBegin());
        assertEquals(31, element.getEnd());
        assertEquals("> ", element.getMarker());
    }
*/

    @Test
    public void parse7() {
        ReferenceElement element = parseString(stringUnknown);
        assertNull(element);

        ReferenceElement element1 = parseString(stringNull);
        assertNull(element1);
    }

    @Test
    public void parse8() {
        Groupchat element = (Groupchat) parseGroupString(stringGroupchat);
        assertNotNull(element);
        assert (element instanceof GroupchatUserContainer);
        GroupchatUserExtension user = ((GroupchatUserContainer)element).getUser();
        assertNotNull(user);
        assertEquals("", user.getBadge());
        assertEquals("kubsgzldk3csvtez", user.getId());
        assertEquals("john.doe@xabber.org", user.getJid());
        assertEquals("john.doe", user.getNickname());
        assertEquals("member", user.getRole());
        assertEquals("http://xabber.org/images/d7072b2bc4652580911649870699787b18.jpeg", user.getAvatar());
    }

    @Test
    public void parse9() {
        Groupchat element = (Groupchat) parseGroupString(stringGroupPresence);
        assertNotNull(element);
        assert (element instanceof GroupchatPresence);
        GroupchatPresence presence = (GroupchatPresence)element;

        assertEquals("My Chat", presence.getName());
        assertEquals("public", presence.getPrivacy());
        assertEquals("1464523434", presence.getPinnedMessageId());
        assertFalse(presence.isCollect());
        assertTrue(presence.isP2p());
        assertEquals(78, presence.getPresentMembers());
        assertEquals(225, presence.getAllMembers());
    }

    private Groupchat parseGroupString(String source) {
        Groupchat result = null;
        try {
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(source));
            result = groupchatProvider.parse(parser, 0);
        } catch (Exception e) {
            fail("Exception while parsing: " + e.toString());
        }
        return result;
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