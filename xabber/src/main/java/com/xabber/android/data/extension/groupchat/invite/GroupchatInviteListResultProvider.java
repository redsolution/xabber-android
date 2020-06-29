package com.xabber.android.data.extension.groupchat.invite;

import com.xabber.android.data.extension.groupchat.GroupchatUserExtension;
import com.xabber.android.data.extension.groupchat.members.GroupchatMembersQueryIQ;

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
                    if (GroupchatUserExtension.ELEMENT.equals(parser.getName())) {
                        String inviteJid = parser.getAttributeValue("", GroupchatUserExtension.ELEMENT_JID);
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
