package com.xabber.android.data.extension.groupchat;

import com.xabber.android.data.extension.references.ReferenceElement;
import com.xabber.android.data.extension.references.ReferencesProvider;
import com.xabber.android.data.extension.references.mutable.groupchat.GroupchatMemberReference;
import com.xabber.android.data.groups.GroupIndexType;
import com.xabber.android.data.groups.GroupMembershipType;
import com.xabber.android.data.groups.GroupPrivacyType;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class GroupchatProvider extends ExtensionElementProvider<GroupchatExtensionElement> {

    @Override
    public GroupchatExtensionElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        GroupchatMemberContainer user = null;
        GroupchatPresence presence;

        outerloop:
        while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (ReferenceElement.ELEMENT.equals(parser.getName())
                            && ReferenceElement.NAMESPACE.equals(parser.getNamespace())) {
                        GroupchatMemberReference referenceWrapperUser = parseGroupUserReference(parser);
                        if (referenceWrapperUser != null)
                            user = new GroupchatMemberContainer(referenceWrapperUser);
                        return user;
                    } else {
                        String name = parser.getName();
                        if (name != null) {
                            for (String field : GroupchatPresence.presenceFields) {
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

    private GroupchatPresence parseGroupPresence(XmlPullParser parser) throws XmlPullParserException, IOException {
        GroupchatPresence presence = new GroupchatPresence();
        int initialDepth = parser.getDepth() - 1;

        innerloop:
        while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    switch (name) {
                        case GroupchatPresence.NAME:
                            presence.setName(parser.nextText());
                            break;
                        case GroupchatPresence.DESCRIPTION:
                            presence.setDescription(parser.nextText());
                            break;
                        case GroupchatPresence.COLLECT:
                            presence.setCollect("yes".equals(parser.nextText()));
                            break;
                        case GroupchatPresence.MEMBERS:
                            presence.setAllMembers(Integer.parseInt(parser.nextText()));
                            break;
                        case GroupchatPresence.PEER_TO_PEER:
                            presence.setP2p("true".equals(parser.nextText()));
                            break;
                        case GroupchatPresence.PINNED_MESSAGE:
                            presence.setPinnedMessageId(parser.nextText());
                            break;
                        case GroupchatPresence.PRESENT:
                            presence.setPresentMembers(Integer.parseInt(parser.nextText()));
                            break;
                        case GroupchatPresence.PRIVACY:
                            presence.setPrivacy(GroupPrivacyType
                                    .fromXml(parser.nextText()));
                            break;
                        case GroupchatPresence.MEMBERSHIP:
                            presence.setMembership(GroupMembershipType
                                    .fromXml(parser.nextText()));
                            break;
                        case GroupchatPresence.INDEX:
                            presence.setIndex(GroupIndexType
                                    .fromXml(parser.nextText()));
                            break;
                        case GroupchatPresence.STATUS:
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
