package com.xabber.android.data.extension.groupchat;

import com.xabber.android.data.extension.android.groupchat.GroupPinMessageIQ;

import org.junit.Before;
import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;

import static org.junit.Assert.assertEquals;

public class PinnedMessageTest {

    String messageId;
    GroupPinMessageIQ iq;

    @Before
    public void setUp(){
        messageId = "messageStanzaId";
        try{

            iq = new GroupPinMessageIQ(JidCreate.fullFrom("to@to.to/to"), messageId);
            iq.setStanzaId("4WqDE-3");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testPinnedMessageIqBuilding(){
        String expected = "<iq to='to@to.to/to' id='4WqDE-3' type='set'><update xmlns='https://xabber.com/protocol/groups'><pinned>messageStanzaId</pinned></update></iq>";
        assertEquals(expected, iq.toXML().toString());
    }

}
