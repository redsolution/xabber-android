package com.xabber.android.data.extension.references;

import com.xabber.android.data.log.LogManager;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.forward.provider.ForwardedProvider;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

public class ReferencesProvider extends ExtensionElementProvider<ReferenceElement> {

    @Override
    public ReferenceElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        String type = null, beginS = null, endS = null, marker = null, uri = null;
        List<Forwarded> forwardedMessages = new ArrayList<>();
        List<RefMedia> mediaElements = new ArrayList<>();
        boolean bold = false, italic = false, underline = false, strike = false;
        RefUser user = null;

        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (ReferenceElement.ELEMENT.equals(parser.getName())
                            && ReferenceElement.NAMESPACE.equals(parser.getNamespace())) {
                        type = parser.getAttributeValue("", ReferenceElement.ATTRIBUTE_TYPE);
                        beginS = parser.getAttributeValue("", ReferenceElement.ATTRIBUTE_BEGIN);
                        endS = parser.getAttributeValue("", ReferenceElement.ATTRIBUTE_END);
                        parser.next();
                    } else if (Forwarded.ELEMENT.equals(parser.getName())
                            && Forwarded.NAMESPACE.equals(parser.getNamespace())) {
                        Forwarded forwarded = ForwardedProvider.INSTANCE.parse(parser);
                        if (forwarded != null) forwardedMessages.add(forwarded);
                        parser.next();
                    } else if (RefMedia.ELEMENT.equals(parser.getName())) {
                        RefMedia media = parseMedia(parser);
                        if (media != null) mediaElements.add(media);
                        parser.next();
                    } else if (RefUser.ELEMENT.equals(parser.getName())
                            && RefUser.NAMESPACE.equals(parser.getNamespace())) {
                        user = parseUser(parser);
                        parser.next();
                    } else if (ReferenceElement.ELEMENT_BOLD.equals(parser.getName())) {
                        bold = true;
                        parser.next();
                    } else if (ReferenceElement.ELEMENT_ITALIC.equals(parser.getName())) {
                        italic = true;
                        parser.next();
                    } else if (ReferenceElement.ELEMENT_UNDERLINE.equals(parser.getName())) {
                        underline = true;
                        parser.next();
                    } else if (ReferenceElement.ELEMENT_STRIKE.equals(parser.getName())) {
                        strike = true;
                        parser.next();
                    } else if (ReferenceElement.ELEMENT_URI.equals(parser.getName())) {
                        uri = parser.nextText();
                    } else if (ReferenceElement.ELEMENT_MARKER.equals(parser.getName())) {
                        marker = parser.nextText();
                    } else parser.next();
                    break;
                case XmlPullParser.END_TAG:
                    if (ReferenceElement.ELEMENT.equals(parser.getName())) {
                        break outerloop;
                    } else parser.next();
                    break;
                default:
                    parser.next();
            }
        }

        int begin = 0, end = 0;
        if (beginS != null && !beginS.isEmpty()) begin = Integer.valueOf(beginS);
        if (endS != null && !endS.isEmpty()) end = Integer.valueOf(endS);

        try {
            ReferenceElement.Type refType = ReferenceElement.Type.valueOf(type);
            switch (refType) {
                case forward:
                    return new Forward(begin, end, forwardedMessages);
                case media:
                    return new Media(begin, end, mediaElements);
                case markup:
                    return new Markup(begin, end, bold, italic, underline, strike, uri);
                case quote:
                    return new Quote(begin, end, marker);
                case mention:
                    return new Mention(begin, end, uri);
                case groupchat:
                    return new Groupchat(begin, end, user);
                default:
                    return null;
            }
        } catch (Exception e) {
            LogManager.d(ReferencesProvider.class, e.toString());
            return null;
        }
    }

    private RefMedia parseMedia(XmlPullParser parser) throws Exception {
        String uri = null;
        RefFile file = null;

        parser.next();
        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (RefFile.ELEMENT.equals(parser.getName())) {
                        file = parseFile(parser);
                        parser.next();
                    }
                    if (RefMedia.ELEMENT_URI.equals(parser.getName())) {
                        uri = parser.nextText();
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (RefMedia.ELEMENT.equals(parser.getName())) {
                        break outerloop;
                    } else parser.next();
                    break;
                default:
                    parser.next();
            }
        }
        if (file != null && uri != null) return new RefMedia(file, uri);
        else return null;
    }

    private RefFile parseFile(XmlPullParser parser) throws Exception {
        RefFile.Builder builder = RefFile.newBuilder();

        parser.next();
        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    switch (parser.getName()) {
                        case RefFile.ELEMENT_MEDIA_TYPE:
                            builder.setMediaType(parser.nextText());
                            break;
                        case RefFile.ELEMENT_NAME:
                            builder.setName(parser.nextText());
                            break;
                        case RefFile.ELEMENT_DESC:
                            builder.setDesc(parser.nextText());
                            break;
                        case RefFile.ELEMENT_HEIGHT:
                            builder.setHeight(Integer.valueOf(parser.nextText()));
                            break;
                        case RefFile.ELEMENT_WIDTH:
                            builder.setWidth(Integer.valueOf(parser.nextText()));
                            break;
                        case RefFile.ELEMENT_SIZE:
                            builder.setSize(Long.valueOf(parser.nextText()));
                            break;
                        case RefFile.ELEMENT_DURATION:
                            builder.setDuration(Long.valueOf(parser.nextText()));
                            break;
                        default:
                            parser.next();
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (RefFile.ELEMENT.equals(parser.getName())) {
                        break outerloop;
                    } else parser.next();
                    break;
                default:
                    parser.next();
            }
        }
        return builder.build();
    }

    private RefUser parseUser(XmlPullParser parser) throws Exception {
        String id = null;
        String jid = null;
        String nickname = null;
        String role = null;
        String badge = null;
        String avatar = null;

        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    switch (parser.getName()) {
                        case RefUser.ELEMENT:
                            id = parser.getAttributeValue("", RefUser.ATTR_ID);
                            parser.next();
                            break;
                        case RefUser.ELEMENT_JID:
                            jid = parser.nextText();
                            break;
                        case RefUser.ELEMENT_BADGE:
                            badge = parser.nextText();
                            break;
                        case RefUser.ELEMENT_NICKNAME:
                            nickname = parser.nextText();
                            break;
                        case RefUser.ELEMENT_ROLE:
                            role = parser.nextText();
                            break;
                        case RefUser.ELEMENT_METADATA:
                            if (RefUser.NAMESPACE_METADATA.equals(parser.getNamespace()))
                            avatar = parseAvatar(parser);
                            parser.next();
                            break;
                        default:
                            parser.next();
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (RefUser.ELEMENT.equals(parser.getName())) {
                        break outerloop;
                    } else parser.next();
                    break;
                default:
                    parser.next();
            }
        }
        if (id != null && nickname != null && role != null) {
            RefUser user = new RefUser(id, nickname, role);
            user.setBadge(badge);
            user.setJid(jid);
            user.setAvatar(avatar);
            return user;
        } else return null;
    }

    private String parseAvatar(XmlPullParser parser) throws Exception {
        String avatar = null;
        parser.next();
        if (parser.getEventType() == XmlPullParser.START_TAG) {
            if (RefUser.ELEMENT_INFO.equals(parser.getName())) {
                avatar = parser.getAttributeValue("", RefUser.ATTR_URL);
                parser.next();
            }
        }
        return avatar;
    }
}
