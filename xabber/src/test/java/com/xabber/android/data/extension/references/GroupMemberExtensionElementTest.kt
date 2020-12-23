package com.xabber.android.data.extension.references

import com.xabber.android.data.extension.groupchat.GroupMemberExtensionElement
import com.xabber.xmpp.avatar.MetadataInfo
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URL

class GroupMemberExtensionElementTest {

    @Test
    fun test_toXml(){
        val groupMemberRightsElement = GroupMemberExtensionElement("memberId", "memberNickname",
                "memberRole")
        assertEquals("<user xmlns='https://xabber.com/protocol/groups' id='memberId'>" +
                    "<role>memberRole</role>" +
                    "<nickname>memberNickname</nickname>" +
                    "<badge></badge>" +
                    "<metadata xmlns='urn:xmpp:avatar:metadata'></metadata>" +
                "</user>",
                groupMemberRightsElement.toXML().toString())

        groupMemberRightsElement.jid = "group_member@jid.domain"
        assertEquals("<user xmlns='https://xabber.com/protocol/groups' id='memberId'>" +
                    "<role>memberRole</role>" +
                    "<nickname>memberNickname</nickname>" +
                    "<badge></badge>" +
                    "<jid>group_member@jid.domain</jid>" +
                    "<metadata xmlns='urn:xmpp:avatar:metadata'></metadata>" +
                "</user>",
                groupMemberRightsElement.toXML().toString())

        groupMemberRightsElement.lastPresent = null
        assertEquals("<user xmlns='https://xabber.com/protocol/groups' id='memberId'>" +
                    "<role>memberRole</role>" +
                    "<nickname>memberNickname</nickname>" +
                    "<badge></badge>" +
                    "<jid>group_member@jid.domain</jid>" +
                    "<metadata xmlns='urn:xmpp:avatar:metadata'></metadata>" +
                "</user>",
                groupMemberRightsElement.toXML().toString())

        groupMemberRightsElement.avatarInfo = MetadataInfo("metadataId", URL("https://url.domain/avatar"),
                1000, "image/jpeg", 0, 0)
        assertEquals("<user xmlns='https://xabber.com/protocol/groups' id='memberId'>" +
                    "<role>memberRole</role>" +
                    "<nickname>memberNickname</nickname>" +
                    "<badge></badge>" +
                    "<jid>group_member@jid.domain</jid>" +
                    "<metadata xmlns='urn:xmpp:avatar:metadata'>" +
                        "<info id='metadataId' bytes='1000' type='image/jpeg' url='https://url.domain/avatar'/>" +
                    "</metadata>" +
                "</user>", groupMemberRightsElement.toXML().toString())
    }

}