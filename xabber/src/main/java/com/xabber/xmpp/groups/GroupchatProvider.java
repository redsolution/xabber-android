package com.xabber.xmpp.groups;

import com.xabber.android.data.extension.references.ReferenceElement;
import com.xabber.android.data.extension.references.ReferencesProvider;
import com.xabber.android.data.extension.references.mutable.groupchat.GroupchatMemberReference;
import com.xabber.android.data.extension.groups.GroupIndexType;
import com.xabber.android.data.extension.groups.GroupMembershipType;
import com.xabber.android.data.extension.groups.GroupPrivacyType;

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
            e.printStackTrace();
        }
        if (element instanceof GroupchatMemberReference) {
            return (GroupchatMemberReference) element;
        } else {
            return null;
        }
    }

    private GroupPresenceExtensionElement parseGroupPresence(XmlPullParser parser) throws XmlPullParserException, IOException {
        GroupPresenceExtensionElement presence = new GroupPresenceExtensionElement();
        int initialDepth = parser.getDepth() - 1;

        innerloop:
        while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    switch (name) {
                        case GroupPresenceExtensionElement.NAME:
                            presence.setName(parser.nextText());
                            break;
                        case GroupPresenceExtensionElement.DESCRIPTION:
                            presence.setDescription(parser.nextText());
                            break;
                        case GroupPresenceExtensionElement.COLLECT:
                            presence.setCollect("yes".equals(parser.nextText()));
                            break;
                        case GroupPresenceExtensionElement.MEMBERS:
                            presence.setAllMembers(Integer.parseInt(parser.nextText()));
                            break;
                        case GroupPresenceExtensionElement.PEER_TO_PEER:
                            presence.setP2p("true".equals(parser.nextText()));
                            break;
                        case GroupPresenceExtensionElement.PINNED_MESSAGE:
                            presence.setPinnedMessageId(parser.nextText());
                            break;
                        case GroupPresenceExtensionElement.PRESENT:
                            presence.setPresentMembers(Integer.parseInt(parser.nextText()));
                            break;
                        case GroupPresenceExtensionElement.PRIVACY:
                            presence.setPrivacy(GroupPrivacyType
                                    .fromXml(parser.nextText()));
                            break;
                        case GroupPresenceExtensionElement.MEMBERSHIP:
                            presence.setMembership(GroupMembershipType
                                    .fromXml(parser.nextText()));
                            break;
                        case GroupPresenceExtensionElement.INDEX:
                            presence.setIndex(GroupIndexType
                                    .fromXml(parser.nextText()));
                            break;
                        case GroupPresenceExtensionElement.STATUS:
                            presence.setStatus(parser.nextText());
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

        return presence;
    }

}
