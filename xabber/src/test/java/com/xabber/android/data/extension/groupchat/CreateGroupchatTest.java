package com.xabber.android.data.extension.groupchat;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.message.chat.groupchat.GroupchatIndexType;
import com.xabber.android.data.message.chat.groupchat.GroupchatMembershipType;
import com.xabber.android.data.message.chat.groupchat.GroupchatPrivacyType;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CreateGroupchatTest {

    String reference;

    AccountJid from;
    String to;
    String description;
    String name;
    String groupJid;
    GroupchatMembershipType membershipType;
    GroupchatIndexType indexType;
    GroupchatPrivacyType privacyType;
    String iqId = "iq_id";

    CreateGroupchatIQ iq;

    @Before
    public void setup(){

        reference = "<iq to='groupchat.server.net' from='account@domain.ru/resource' id='iq_id' type='set'>" +
                    "<create xmlns='http://xabber.com/protocol/groupchat'>" +
                        "<name>Group Name</name>" +
                        "<description>my_group</description>" +
                        "<jid>Groupchat description</jid>" +
                        "<membership>open</membership>" +
                        "<privacy>incognito</privacy>" +
                        "<index>local</index>" +
                        "<contacts>" +
                            "<contact>account@domain.ru</contact>" +
                        "</contacts>" +
                    "</create>" +
                "</iq>";

        try {
            from = AccountJid.from("account@domain.ru/resource");
        } catch (Exception e){
            e.printStackTrace();
        }

        to = "groupchat.server.net";
        description = "Groupchat description";
        name = "Group Name";
        groupJid = "my_group";
        membershipType = GroupchatMembershipType.OPEN;
        indexType = GroupchatIndexType.LOCAL;
        privacyType = GroupchatPrivacyType.INCOGNITO;

        iq = new CreateGroupchatIQ(from.getFullJid(), to, name, description, groupJid,
                membershipType, privacyType, indexType);

        iq.setStanzaId("iq_id");
    }


    @Test
    public void testCreateGroupchatIqCreating(){
        assertEquals(reference, iq.toXML().toString());
    }

}
