package com.xabber.android.data.extension.groupchat;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.android.groupchat.create.CreateGroupchatIQ;
import com.xabber.android.data.groups.GroupIndexType;
import com.xabber.android.data.groups.GroupMembershipType;
import com.xabber.android.data.groups.GroupPrivacyType;

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
    GroupMembershipType membershipType;
    GroupIndexType indexType;
    GroupPrivacyType privacyType;

    CreateGroupchatIQ iq;

    @Before
    public void setup(){

        reference = "<iq to='groupchat.server.net' from='account@domain.ru/resource' id='iq_id' type='set'><query xmlns='https://xabber.com/protocol/groups#create'><name>Group Name</name><description>Groupchat description</description><membership>open</membership><privacy>incognito</privacy><index>local</index><contacts><contact>account@domain.ru</contact></contacts><localpart>my_group</localpart></query></iq>";

        try {
            from = AccountJid.from("account@domain.ru/resource");
        } catch (Exception e){
            e.printStackTrace();
        }

        to = "groupchat.server.net";
        description = "Groupchat description";
        name = "Group Name";
        groupJid = "my_group";
        membershipType = GroupMembershipType.OPEN;
        indexType = GroupIndexType.LOCAL;
        privacyType = GroupPrivacyType.INCOGNITO;

        iq = new CreateGroupchatIQ(from.getFullJid(), to, name, groupJid, description,
                membershipType, privacyType, indexType);

        iq.setStanzaId("iq_id");
    }

    @Test
    public void testCreateGroupchatIqCreating(){
        assertEquals(reference, iq.toXML().toString());
    }

}
