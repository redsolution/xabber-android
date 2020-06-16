package com.xabber.android.data.extension.groupchat;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PinnedMessageTest {

    GroupchatPinnedMessageElement element;
    GroupchatUpdateIQ iq;

    @Before
    public void setUp(){
        element = new GroupchatPinnedMessageElement("stanzaId");
        iq = new GroupchatUpdateIQ("from", "to", element);
    }

    @Test
    public void testPinnedMessageElementBuilding(){
        assertEquals("<pinned-message>stanzaId</pinned-message>", element.toXML().toString());
    }

    @Test
    public void testPinnedMessageIqBuilding(){
        String expected = "<iq to='to' from='from' id='" + iq.getStanzaId() + "' type='set'>" +
                "<update xmlns='http://xabber.com/protocol/groupchat'>" +
                "<pinned-message>stanzaId</pinned-message>" +
                "</update>" +
                "</iq>";
        assertEquals(expected, iq.toXML().toString());
    }

}
