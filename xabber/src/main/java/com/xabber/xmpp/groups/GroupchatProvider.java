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

public class GroupchatProvider extends ExtensionElementProvider<GroupchatExtensionElement> {

    @Override
    public GroupchatExtensionElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        GroupchatMemberContainerExtensionElement user = null;
        GroupchatPresenceExtensionElement presence;

        outerloop:
        while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (ReferenceElement.ELEMENT.equals(parser.getName())
                            && ReferenceElement.NAMESPACE.equals(parser.getNamespace())) {
                        GroupchatMemberReference referenceWrapperUser = parseGroupUserReference(parser);
                        if (referenceWrapperUser != null)
                            user = new GroupchatMemberContainerExtensionElement(referenceWrapperUser);
                        return user;
                    } else {
                        String name = parser.getName();
                        if (name != null) {
                            for (String field : GroupchatPresenceExtensionElement.presenceFields) {
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
                    if (GroupchatExtensionElement.ELEMENT.equals(parser.getName())
                            && GroupchatExtensionElement.NAMESPACE.equals(parser.getNamespace())) {
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

    private GroupchatPresenceExtensionElement parseGroupPresence(XmlPullParser parser) throws XmlPullParserException, IOException {
        GroupchatPresenceExtensionElement presence = new GroupchatPresenceExtensionElement();
        int initialDepth = parser.getDepth() - 1;

        innerloop:
        while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    switch (name) {
                        case GroupchatPresenceExtensionElement.NAME:
                            presence.setName(parser.nextText());
                            break;
                        case GroupchatPresenceExtensionElement.DESCRIPTION:
                            presence.setDescription(parser.nextText());
                            break;
                        case GroupchatPresenceExtensionElement.COLLECT:
                            presence.setCollect("yes".equals(parser.nextText()));
                            break;
                        case GroupchatPresenceExtensionElement.MEMBERS:
                            presence.setAllMembers(Integer.parseInt(parser.nextText()));
                            break;
                        case GroupchatPresenceExtensionElement.PEER_TO_PEER:
                            presence.setP2p("true".equals(parser.nextText()));
                            break;
                        case GroupchatPresenceExtensionElement.PINNED_MESSAGE:
                            presence.setPinnedMessageId(parser.nextText());
                            break;
                        case GroupchatPresenceExtensionElement.PRESENT:
                            presence.setPresentMembers(Integer.parseInt(parser.nextText()));
                            break;
                        case GroupchatPresenceExtensionElement.PRIVACY:
                            presence.setPrivacy(GroupPrivacyType
                                    .fromXml(parser.nextText()));
                            break;
                        case GroupchatPresenceExtensionElement.MEMBERSHIP:
                            presence.setMembership(GroupMembershipType
                                    .fromXml(parser.nextText()));
                            break;
                        case GroupchatPresenceExtensionElement.INDEX:
                            presence.setIndex(GroupIndexType
                                    .fromXml(parser.nextText()));
                            break;
                        case GroupchatPresenceExtensionElement.STATUS:
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
