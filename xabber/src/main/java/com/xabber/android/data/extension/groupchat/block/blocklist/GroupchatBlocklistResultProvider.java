package com.xabber.android.data.extension.groupchat.block.blocklist;

import com.xabber.android.data.extension.groupchat.members.GroupchatMembersQueryIQ;

import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

public class GroupchatBlocklistResultProvider extends IQProvider<GroupchatBlocklistResultIQ> {

    @Override
    public GroupchatBlocklistResultIQ parse(XmlPullParser parser, int initialDepth) throws Exception {
        GroupchatBlocklistResultIQ resultIQ = new GroupchatBlocklistResultIQ();
        ArrayList<GroupchatBlocklistItemElement> blockedItems = new ArrayList<>();

        outerloop:
        while (true) {
            int event = parser.getEventType();
            switch (event) {
                case XmlPullParser.START_TAG:
                    switch (parser.getName()) {
                        case GroupchatBlocklistResultIQ.ELEMENT_JID:
                            String blockedJid = parser.nextText();
                            if (blockedJid != null) {
                                blockedItems.add(new GroupchatBlocklistItemElement(
                                        GroupchatBlocklistItemElement.ItemType.jid,
                                        blockedJid
                                ));
                            }
                            break;
                            ////////////////////// TODO check which is wrong: documentation or server realization
                        case "user":
                            String userJid = parser.getAttributeValue("", "jid");
                            if (userJid != null) {
                                blockedItems.add(new GroupchatBlocklistItemElement(
                                        GroupchatBlocklistItemElement.ItemType.jid,
                                        userJid
                                ));
                            }
                            //////////////////////
                        case GroupchatBlocklistResultIQ.ELEMENT_DOMAIN:
                            String blockedDomain = parser.getAttributeValue("",
                                    GroupchatBlocklistResultIQ.ATTRIBUTE_NAME);
                            if (blockedDomain != null) {
                                blockedItems.add(new GroupchatBlocklistItemElement(
                                        GroupchatBlocklistItemElement.ItemType.domain,
                                        blockedDomain
                                ));
                            }
                            break;
                        case GroupchatBlocklistResultIQ.ELEMENT_ID:
                            String blockedUserId = parser.nextText();
                            if (blockedUserId != null) {
                                blockedItems.add(new GroupchatBlocklistItemElement(
                                        GroupchatBlocklistItemElement.ItemType.id,
                                        blockedUserId
                                ));
                            }
                            break;
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

        resultIQ.setBlockedItems(blockedItems);
        return resultIQ;
    }


}
