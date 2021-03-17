package com.xabber.xmpp.groups.invite.outgoing;

import com.xabber.xmpp.groups.GroupMemberExtensionElement;
import com.xabber.xmpp.groups.members.GroupchatMembersQueryIQ;

import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

public class GroupchatInviteListResultProvider extends IQProvider<GroupchatInviteListResultIQ> {

    @Override
    public GroupchatInviteListResultIQ parse(XmlPullParser parser, int initialDepth) throws Exception {
        GroupchatInviteListResultIQ resultIQ = new GroupchatInviteListResultIQ();
        ArrayList<String> listOfInvites = new ArrayList<>();

        outerloop:
        while (true) {
            int event = parser.getEventType();
            switch (event) {
                case XmlPullParser.START_TAG:
                    if (GroupMemberExtensionElement.ELEMENT.equals(parser.getName())) {
                        String inviteJid = parser.getAttributeValue("", GroupMemberExtensionElement.ELEMENT_JID);
                        if (inviteJid != null) {
                            listOfInvites.add(inviteJid);
                        }
                    }
                    parser.next();
                    break;
                case XmlPullParser.END_TAG:
                    if (GroupchatMembersQueryIQ.ELEMENT.equals(parser.getName())) {
                        break outerloop;
                    } else parser.next();
                    break;
                default:
                    parser.next();
            }
        }

        resultIQ.setListOfInvitedJids(listOfInvites);
        return resultIQ;
    }
}
