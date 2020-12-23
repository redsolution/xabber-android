package com.xabber.android.data.extension.groupchat.members;

import com.xabber.android.data.extension.groupchat.GroupMemberExtensionElement;
import com.xabber.xmpp.avatar.MetadataInfo;
import com.xabber.xmpp.avatar.MetadataProvider;

import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Collection;

public class GroupchatMembersResultProvider extends IQProvider<GroupchatMembersQueryIQ> {

    @Override
    public GroupchatMembersQueryIQ parse(XmlPullParser parser, int initialDepth) throws Exception {
        GroupchatMembersResultIQ resultIQ = new GroupchatMembersResultIQ();
        Collection<GroupMemberExtensionElement> listOfMembers = new ArrayList<>();

        outerloop:
        while (true) {
            int event = parser.getEventType();
            switch (event) {
                case XmlPullParser.START_TAG:
                    switch (parser.getName()) {
                        case GroupchatMembersQueryIQ.ELEMENT:
                            resultIQ.setQueryId(parser.getAttributeValue("",
                                    GroupchatMembersQueryIQ.MEMBER_ID));
                            resultIQ.setQueryVersion(parser.getAttributeValue("",
                                    GroupchatMembersQueryIQ.VERSION));

                            parser.next();
                            break;
                        case GroupMemberExtensionElement.ELEMENT:
                            GroupMemberExtensionElement member = parseUser(parser);
                            if (member != null) listOfMembers.add(member);
                            parser.next();
                            break;
                        default:
                            parser.next();
                    }
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

        resultIQ.setListOfMembers(listOfMembers);
        return resultIQ;
    }

    private GroupMemberExtensionElement parseUser(XmlPullParser parser) throws Exception {
        String id = null;
        String jid = null;
        String nickname = null;
        String role = null;
        String badge = null;
        String present = null;
        String subscription = null;
        MetadataInfo avatar = null;

        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    switch (parser.getName()) {
                        case GroupMemberExtensionElement.ELEMENT:
                            id = parser.getAttributeValue("", GroupMemberExtensionElement.ATTR_ID);
                            parser.next();
                            break;
                        case GroupMemberExtensionElement.ELEMENT_JID:
                            jid = parser.nextText();
                            break;
                        case GroupMemberExtensionElement.ELEMENT_BADGE:
                            badge = parser.nextText();
                            break;
                        case GroupMemberExtensionElement.ELEMENT_NICKNAME:
                            nickname = parser.nextText();
                            break;
                        case GroupMemberExtensionElement.ELEMENT_ROLE:
                            role = parser.nextText();
                            break;
                        case GroupMemberExtensionElement.ELEMENT_PRESENT:
                            present = parser.nextText();
                            break;
                        case GroupMemberExtensionElement.ELEMENT_SUBSCRIPTION:
                            subscription = parser.nextText();
                            break;
                        case GroupMemberExtensionElement.ELEMENT_METADATA:
                            if (GroupMemberExtensionElement.NAMESPACE_METADATA.equals(parser.getNamespace()))
                                avatar = parseAvatar(parser);
                            parser.next();
                            break;
                        default:
                            parser.next();
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (GroupMemberExtensionElement.ELEMENT.equals(parser.getName())) {
                        break outerloop;
                    } else parser.next();
                    break;
                default:
                    parser.next();
            }
        }
        if (id != null && nickname != null && role != null) {
            GroupMemberExtensionElement user = new GroupMemberExtensionElement(id, nickname, role);
            user.setBadge(badge);
            user.setJid(jid);
            user.setLastPresent(present);
            user.setAvatarInfo(avatar);
            user.setSubscription(subscription);
            return user;
        } else return null;
    }

    private MetadataInfo parseAvatar(XmlPullParser parser) throws Exception {
        parser.next();
        if (parser.getEventType() == XmlPullParser.START_TAG) {
            if (GroupMemberExtensionElement.ELEMENT_INFO.equals(parser.getName())) {
                return MetadataProvider.parseInfo(parser);
            }
        }
        return null;
    }

}
