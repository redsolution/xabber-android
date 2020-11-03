package com.xabber.android.data.extension.groupchat;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PinnedMessageTest {

    String messageId;
    GroupPinMessageIQ iq;
    AccountJid accountJid;
    ContactJid contactJid;

    @Before
    public void setUp(){
        messageId = "messageStanzaId";
        try{
            accountJid = AccountJid.from("from@from.from/from");
            contactJid = ContactJid.from("to@to.to/to");

            iq = new GroupPinMessageIQ(accountJid.getFullJid(), contactJid.getBareJid(), messageId);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    @Test
    public void testPinnedMessageIqBuilding(){
        String expected = "<iq to='" + contactJid.getBareJid().toString() +  "' from='" + accountJid.toString() + "' id='" + iq.getStanzaId() + "' type='set'><update xmlns='https://xabber.com/protocol/groups'><pinned>messageStanzaId</pinned></update></iq>";
        assertEquals(expected, iq.toXML().toString());
    }

}
