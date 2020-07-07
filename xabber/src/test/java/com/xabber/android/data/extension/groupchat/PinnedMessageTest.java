package com.xabber.android.data.extension.groupchat;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PinnedMessageTest {

    GroupchatPinnedMessageElement element;
    GroupchatUpdateIQ iq;
    AccountJid accountJid;
    ContactJid contactJid;

    @Before
    public void setUp(){
        element = new GroupchatPinnedMessageElement("stanzaId");
        try{
            accountJid = AccountJid.from("from@from.from/from");
            contactJid = ContactJid.from("tp@to.to/to");
            iq = new GroupchatUpdateIQ(accountJid.getFullJid(), contactJid.getBareJid(), element);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    @Test
    public void testPinnedMessageElementBuilding(){
        assertEquals("<pinned-message>stanzaId</pinned-message>", element.toXML().toString());
    }

    @Test
    public void testPinnedMessageIqBuilding(){
        String expected = "<iq to='" + accountJid.getFullJid().toString() + "' from='" + contactJid.getBareJid().toString() + "' id='" + iq.getStanzaId() + "' type='set'>" +
                "<update xmlns='http://xabber.com/protocol/groupchat'>" +
                "<pinned-message>stanzaId</pinned-message>" +
                "</update>" +
                "</iq>";
        assertEquals(expected, iq.toXML().toString());
    }

}
