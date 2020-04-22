package com.xabber.android.data.extension.groupchat;

import com.xabber.android.data.extension.references.ReferenceElement;
import com.xabber.android.data.extension.references.ReferencesProvider;
import com.xabber.android.data.extension.references.mutable.groupchat.GroupchatUserReference;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class GroupchatProvider extends ExtensionElementProvider<Groupchat> {

/*

*/

    @Override
    public Groupchat parse(XmlPullParser parser, int initialDepth) throws Exception {
        GroupchatUserContainer user = null;
        GroupchatPresence presence;

        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (ReferenceElement.ELEMENT.equals(parser.getName())
                            && ReferenceElement.NAMESPACE.equals(parser.getNamespace())) {
                        GroupchatUserReference referenceWrapperUser = parseGroupUserReference(parser);
                        if (referenceWrapperUser != null) user = new GroupchatUserContainer(referenceWrapperUser);
                        return user;
                    } else if (GroupchatPresence.NAME.equals(parser.getName()) ||
                            GroupchatPresence.COLLECT.equals(parser.getName()) ||
                            GroupchatPresence.MEMBERS.equals(parser.getName()) ||
                            GroupchatPresence.PEER_TO_PEER.equals(parser.getName()) ||
                            GroupchatPresence.PINNED_MESSAGE.equals(parser.getName()) ||
                            GroupchatPresence.PRESENT.equals(parser.getName()) ||
                            GroupchatPresence.PRIVACY.equals(parser.getName()) ||
                            GroupchatPresence.STATUS.equals(parser.getName())) {
                        presence = parseGroupPresence(parser);
                        return presence;
                    } else parser.next();
                    break;
                case XmlPullParser.END_TAG:
                    if (Groupchat.ELEMENT.equals(parser.getName())
                            && Groupchat.NAMESPACE.equals(parser.getNamespace())) {
                        break outerloop;
                    } else parser.next();
                    break;
                default:
                    parser.next();
            }
        }
        return null;
    }

    private GroupchatUserReference parseGroupUserReference(XmlPullParser parser) {
        ReferenceElement element = null;
        try {
            element = ReferencesProvider.INSTANCE.parse(parser, parser.getDepth());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (element instanceof GroupchatUserReference) {
            return (GroupchatUserReference) element;
        } else {
            return null;
        }
    }

    private GroupchatPresence parseGroupPresence(XmlPullParser parser) throws XmlPullParserException, IOException {
        GroupchatPresence presence = new GroupchatPresence();
        int initialDepth = parser.getDepth() - 1;

        innerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    switch (name) {
                        case GroupchatPresence.NAME:
                            presence.setName(parser.nextText());
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
                            presence.setPrivacy(parser.nextText());
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
