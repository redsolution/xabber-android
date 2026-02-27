package com.xabber.xmpp.groups;

import com.xabber.android.data.extension.references.ReferenceElement;
import com.xabber.android.data.extension.references.ReferencesProvider;
import com.xabber.android.data.extension.references.mutable.groupchat.GroupchatMemberReference;
import com.xabber.android.data.extension.groups.GroupIndexType;
import com.xabber.android.data.extension.groups.GroupMembershipType;
import com.xabber.android.data.extension.groups.GroupPrivacyType;
import com.xabber.android.data.log.LogManager;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class GroupchatProvider extends ExtensionElementProvider<GroupExtensionElement> {

    @Override
    public GroupExtensionElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        GroupMemberContainerExtensionElement user = null;
        GroupPresenceExtensionElement presence;

        outerloop:
        while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (ReferenceElement.ELEMENT.equals(parser.getName())
                            && ReferenceElement.NAMESPACE.equals(parser.getNamespace())) {
                        GroupchatMemberReference referenceWrapperUser = parseGroupUserReference(parser);
                        if (referenceWrapperUser != null)
                            user = new GroupMemberContainerExtensionElement(referenceWrapperUser);
                        return user;
                    } else {
                        String name = parser.getName();
                        if (name != null) {
                            for (String field : GroupPresenceExtensionElement.presenceFields) {
                                if(name.equals(field)) {
                                    presence = parseGroupPresence(parser);
                                    return presence;
                                }
                            }
                        }
                    }
                    parser.next();
                    break;
                case XmlPullParser.END_TAG:
                    if (GroupExtensionElement.ELEMENT.equals(parser.getName())
                            && GroupExtensionElement.NAMESPACE.equals(parser.getNamespace())) {
                        break outerloop;
                    } else parser.next();
                    break;
                default:
                    parser.next();
            }
        }
        return null;
    }

    private GroupchatMemberReference parseGroupUserReference(XmlPullParser parser) {
        ReferenceElement element = null;
        try {
            element = ReferencesProvider.INSTANCE.parse(parser, parser.getDepth());
        } catch (Exception e) {
            LogManager.exception(getClass().getSimpleName(), e);
        }
        if (element instanceof GroupchatMemberReference) {
            return (GroupchatMemberReference) element;
        } else {
            return null;
        }
    }

    private GroupPresenceExtensionElement parseGroupPresence(XmlPullParser parser) throws XmlPullParserException, IOException {
        
        String groupName = null;
        String description = null;
        String status = null;
        String pinnedMessageId = null;
        
        int membersCount = 0;
        int membersOnlineCount = 0;
        
        boolean isCollect = false;
        boolean isPtp = false;
        
        GroupPrivacyType privacyType = null;
        GroupMembershipType membershipType = null;
        GroupIndexType indexType = null;
        
        int initialDepth = parser.getDepth() - 1;
        innerloop:
        while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    switch (name) {
                        case GroupPresenceExtensionElement.NAME:
                            groupName = parser.nextText();
                            break;
                        case GroupPresenceExtensionElement.DESCRIPTION:
                            description = parser.nextText();
                            break;
                        case GroupPresenceExtensionElement.COLLECT:
                            isCollect = "yes".equals(parser.nextText());
                            break;
                        case GroupPresenceExtensionElement.MEMBERS:
                            membersCount = Integer.parseInt(parser.nextText());
                            break;
                        case GroupPresenceExtensionElement.PEER_TO_PEER:
                            isPtp = "true".equals(parser.nextText());
                            break;
                        case GroupPresenceExtensionElement.PINNED_MESSAGE:
                            pinnedMessageId = parser.nextText();
                            break;
                        case GroupPresenceExtensionElement.PRESENT:
                            membersOnlineCount = Integer.parseInt(parser.nextText());
                            break;
                        case GroupPresenceExtensionElement.PRIVACY:
                            privacyType = GroupPrivacyType.fromXml(parser.nextText());
                            break;
                        case GroupPresenceExtensionElement.MEMBERSHIP:
                            membershipType = GroupMembershipType.fromXml(parser.nextText());
                            break;
                        case GroupPresenceExtensionElement.INDEX:
                            indexType = GroupIndexType.fromXml(parser.nextText());
                            break;
                        case GroupPresenceExtensionElement.STATUS:
                            status = parser.nextText();
                            break;
                        default:
                            parser.next();
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getDepth() == initialDepth) {
                        break innerloop;
                    } else parser.next();
                    break;
                default:
                    parser.next();
            }
        }

        return new GroupPresenceExtensionElement(groupName, description, privacyType, membershipType, indexType,
                pinnedMessageId, isCollect, isPtp, status, membersOnlineCount, membersCount);
    }

}
