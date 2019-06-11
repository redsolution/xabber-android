package com.xabber.android.data.extension.references;

import com.xabber.android.data.TestApplication;

import org.jivesoftware.smack.packet.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(application = TestApplication.class)
public class ReferencesManagerTest {

    private String body1, body2, body3, body4;
    private Message message1, message2, message3, message4;

    @Before
    public void setUp() throws Exception {
        body1 = "> Wednesday, June 5, 2019\n> [11:08:45] Валерий Миллер:\n> один\nдва";

        message1 = new Message("test@jabber.com", body1);
        message1.addExtension(new Forward(0, 70, null));

        // -------

        body2 = "https://upload02.xabber.org/5ff2744e91/iKIlTIyZ/guide.txt\nhello";

        message2 = new Message("test@jabber.com", body2);
        message2.addExtension(new Data(0, 57, null));

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
        message4.addExtension(new Quote(0, 37, 5));
    }

    @Test
    public void modifyBodyWithReferences1() {
        assertEquals("Тест <b><i>форматирования</i></b> <i>текста</i>. <u>Использование</u> <strike>нескольких</strike> стилей.",
                ReferencesManager.modifyBodyWithReferences(message3, body3));
    }

    @Test
    public void modifyBodyWithReferences2() {
        assertEquals("два", ReferencesManager.modifyBodyWithReferences(message1, body1));
    }

    @Test
    public void modifyBodyWithReferences3() {
        assertEquals("hello", ReferencesManager.modifyBodyWithReferences(message2, body2));
    }

    @Test
    public void modifyBodyWithReferences4() {
        String expected = "This is a quote\n" +
                          "of two lines\n" +
                          "Hello world!";
        assertEquals(expected, ReferencesManager.modifyBodyWithReferences(message4, body4));
    }
}