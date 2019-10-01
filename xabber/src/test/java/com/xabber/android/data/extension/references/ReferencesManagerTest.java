package com.xabber.android.data.extension.references;

import android.util.Pair;

import com.xabber.android.data.TestApplication;

import org.jivesoftware.smack.packet.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = TestApplication.class)
public class ReferencesManagerTest {

    private String body1, body2, body3, body4, body5, body6, body7, body8, body9;
    private Message message1, message2, message3, message4, message5, message6, message7, message8, message9;

    @Before
    public void setUp() throws Exception {
        body1 = "> Wednesday, June 5, 2019\n> [11:08:45] Валерий Миллер:\n> один\nдва";

        message1 = new Message("test@jabber.com", body1);
        message1.addExtension(new Forward(0, 70, null));

        // -------

        body2 = "https://upload02.xabber.org/5ff2744e91/iKIlTIyZ/guide.txt\nhello";

        message2 = new Message("test@jabber.com", body2);
        message2.addExtension(new Media(0, 57, null));

        // -------

        body3 = "Тест форматирования текста. Использование нескольких стилей.";

        message3 = new Message("test@jabber.com", body3);
        message3.addExtension(new Markup(5, 18, true, true, false, false, null));
        message3.addExtension(new Markup(20, 25, false, true, false, false, null));
        message3.addExtension(new Markup(28, 40, false, false, true, false, null));
        message3.addExtension(new Markup(42, 51, false, false, false, true, null));

        // -------

        body4 = "> This is a quote\n" +
                "> of two lines\n" +
                "Hello world!";

        message4 = new Message("test@jabber.com", body4);
        message4.addExtension(new Quote(0, 37, "> "));

        // -------

        body5 = "Тест > форматирования текста. <b>";

        message5 = new Message("test@jabber.com", body5);
        message5.addExtension(new Markup(10, 23, true, true, false, false, null));

        // -------

        body6 = ">> 😄😃😀 привет";

        message6 = new Message("test@jabber.com", body6);
        message6.addExtension(new Markup(13, 18, true, false, false, false, null));

        // -------

        body7 = "Тест форматирования текста";

        message7 = new Message("test@jabber.com", body7);
        message7.addExtension(new Markup(20, 26, false, false, false, false, "www.xabber.com"));

        // -------

        body8 = "Пользователь, привет!";

        message8 = new Message("test@jabber.com", body8);
        message8.addExtension(new Mention(0, 11, "xmpp:test@jabber.com"));

        // -------

        body9 = "John Doe:\nHello from groupchat!";

        message9 = new Message("test@jabber.com", body9);
        message9.addExtension(new Media(0, 9, null));
    }

    @Test
    public void modifyBodyWithReferences1() {
        Pair<String, String> result = ReferencesManager.modifyBodyWithReferences(message1, body1);
        assertEquals("два", result.first);
        assertNull(result.second);
    }

    @Test
    public void modifyBodyWithReferences2() {
        Pair<String, String> result = ReferencesManager.modifyBodyWithReferences(message2, body2);
        assertEquals("hello", result.first);
        assertNull(result.second);
    }

    @Test
    public void modifyBodyWithReferences3() {
        Pair<String, String> result = ReferencesManager.modifyBodyWithReferences(message3, body3);
        assertEquals("Тест форматирования текста. Использование нескольких стилей.", result.first);
        assertEquals("Тест <b><i>форматирования</i></b> <i>текста</i>. " +
                "<u>Использование</u> <strike>нескольких</strike> стилей.", result.second);
    }

    @Test
    public void modifyBodyWithReferences4() {
        String expected = "<font color='#9e9e9e'>\u2503</font> This is a quote\n" +
                          "<font color='#9e9e9e'>\u2503</font> of two lines\n" +
                          "Hello world!";
        Pair<String, String> result = ReferencesManager.modifyBodyWithReferences(message4, body4);
        assertEquals(body4, result.first);
        assertEquals(expected, result.second);
    }

    @Test
    public void modifyBodyWithReferences5() {
        Pair<String, String> result = ReferencesManager.modifyBodyWithReferences(message5, body5);
        assertEquals("Тест > форматирования текста. <b>", result.first);
        assertEquals("Тест &gt; <b><i>форматирования</i></b> текста. &lt;b&gt;", result.second);
    }

    @Test
    public void modifyBodyWithReferences6() {
        Pair<String, String> result = ReferencesManager.modifyBodyWithReferences(message6, body6);
        assertEquals(">> 😄😃😀 привет", result.first);
        assertEquals("&gt;&gt; 😄😃😀 <b>привет</b>", result.second);
    }

    @Test
    public void modifyBodyWithReferences7() {
        Pair<String, String> result = ReferencesManager.modifyBodyWithReferences(message7, body7);
        assertEquals("Тест форматирования текста", result.first);
        assertEquals("Тест форматирования &zwj;<click uri='www.xabber.com' type='hyperlink'>текста</click>", result.second);
    }

    @Test
    public void modifyBodyWithReferences8() {
        Pair<String, String> result = ReferencesManager.modifyBodyWithReferences(message8, body8);
        assertEquals("Пользователь, привет!", result.first);
        assertEquals("&zwj;<click uri='xmpp:test@jabber.com' type='mention'>Пользователь</click>, привет!", result.second);
    }

    @Test
    public void modifyBodyWithReferences9() {
        Pair<String, String> result = ReferencesManager.modifyBodyWithReferences(message9, body9);
        assertEquals("Hello from groupchat!", result.first);
        assertNull(result.second);
    }

}