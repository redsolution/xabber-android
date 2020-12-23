package com.xabber.android.data.extension.references;

import com.xabber.android.data.extension.groupchat.GroupMemberExtensionElement;
import com.xabber.android.data.extension.references.decoration.Decoration;
import com.xabber.android.data.extension.references.decoration.Markup;
import com.xabber.android.data.extension.references.mutable.Forward;
import com.xabber.android.data.extension.references.mutable.filesharing.FileInfo;
import com.xabber.android.data.extension.references.mutable.filesharing.FileReference;
import com.xabber.android.data.extension.references.mutable.filesharing.FileSharingExtension;
import com.xabber.android.data.extension.references.mutable.filesharing.FileSources;
import com.xabber.android.data.extension.references.mutable.groupchat.GroupchatMemberReference;
import com.xabber.android.data.extension.references.mutable.voice.VoiceMessageExtension;
import com.xabber.android.data.extension.references.mutable.voice.VoiceReference;
import com.xabber.android.data.log.LogManager;
import com.xabber.xmpp.avatar.MetadataInfo;
import com.xabber.xmpp.avatar.MetadataProvider;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.forward.provider.ForwardedProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReferencesProvider extends ExtensionElementProvider<ReferenceElement> {

    public static ReferencesProvider INSTANCE = new ReferencesProvider();

    @Override
    public ReferenceElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        String type = null, beginS = null, endS = null;

        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (ReferenceElement.ELEMENT.equals(parser.getName())
                            && ReferenceElement.NAMESPACE.equals(parser.getNamespace())) {
                        type = parser.getAttributeValue("", ReferenceElement.ATTRIBUTE_TYPE);
                        beginS = parser.getAttributeValue("", ReferenceElement.ATTRIBUTE_BEGIN);
                        endS = parser.getAttributeValue("", ReferenceElement.ATTRIBUTE_END);
                        break outerloop;
                    } else parser.next();
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
        if (beginS != null && !beginS.isEmpty()) begin = Integer.parseInt(beginS);
        if (endS != null && !endS.isEmpty()) end = Integer.parseInt(endS);

        try {
            if (type != null) {
                switch (type) {
                    case "decoration":
                        return parseDecoration(parser, begin, end);
                    case "mutable":
                        return parseMutable(parser, begin, end);
                    case "data":
                        return parseData(parser);
                    default:
                        return null;
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            LogManager.exception(ReferencesProvider.class, e);
            return null;
        }
    }

    private ReferenceElement parseMutable(XmlPullParser parser, int begin, int end) throws Exception {
        List<FileSharingExtension> fileSharingExtensions = new ArrayList<>();
        List<VoiceMessageExtension> voiceMessageExtensions = new ArrayList<>();
        List<Forwarded> forwardedMessages = new ArrayList<>();
        GroupMemberExtensionElement user = null;

        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (Forwarded.ELEMENT.equals(parser.getName())
                            && Forwarded.NAMESPACE.equals(parser.getNamespace())) {
                        Forwarded forwarded = ForwardedProvider.INSTANCE.parse(parser);
                        if (forwarded != null) forwardedMessages.add(forwarded);
                        parser.next();
                    } else if (FileSharingExtension.FILE_SHARING_ELEMENT.equals(parser.getName())
                            && FileSharingExtension.NAMESPACE.equals(parser.getNamespace())) {
                        FileSharingExtension fileSharingExtension = parseFileSharing(parser);
                        if (fileSharingExtension != null) fileSharingExtensions.add(fileSharingExtension);
                    } else if (VoiceMessageExtension.VOICE_ELEMENT.equals(parser.getName())
                            && VoiceMessageExtension.VOICE_NAMESPACE.equals(parser.getNamespace())) {
                        VoiceMessageExtension voiceMessageExtension = parseVoiceSharing(parser);
                        if (voiceMessageExtension != null) voiceMessageExtensions.add(voiceMessageExtension);
                    } else if (GroupMemberExtensionElement.ELEMENT.equals(parser.getName())
                            && GroupMemberExtensionElement.NAMESPACE.equals(parser.getNamespace())) {
                        user = parseUser(parser);
                        parser.next();
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

        if (!forwardedMessages.isEmpty()) {
            return new Forward(begin, end, forwardedMessages);
        } else if (user != null) {
            return new GroupchatMemberReference(begin, end, user);
        } else if (!voiceMessageExtensions.isEmpty()) {
            return new VoiceReference(begin, end, voiceMessageExtensions);
        } else if (!fileSharingExtensions.isEmpty()) {
            return new FileReference(begin, end, fileSharingExtensions);
        } else {
            return null;
        }
    }
    private ReferenceElement parseDecoration(XmlPullParser parser, int begin, int end) throws Exception {
        boolean bold = false, italic = false, underline = false, strike = false, quote = false;
        String link = null;

        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (Decoration.ELEMENT_BOLD.equals(parser.getName())) {
                        bold = true;
                        parser.next();
                    } else if (Decoration.ELEMENT_ITALIC.equals(parser.getName())) {
                        italic = true;
                        parser.next();
                    } else if (Decoration.ELEMENT_UNDERLINE.equals(parser.getName())) {
                        underline = true;
                        parser.next();
                    } else if (Decoration.ELEMENT_STRIKE.equals(parser.getName())) {
                        strike = true;
                        parser.next();
                    } else if (Decoration.ELEMENT_QUOTE.equals(parser.getName())) {
                        quote = true;
                        parser.next();
                    } else if (Decoration.ELEMENT_LINK.equals(parser.getName())) {
                        link = parser.nextText();
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
        return new Markup(begin, end, bold, italic, underline, strike, quote, link);
    }
    private ReferenceElement parseData(XmlPullParser parser) {
        return null;
    }

    private VoiceMessageExtension parseVoiceSharing(XmlPullParser parser) throws Exception {
        VoiceMessageExtension voiceMessageExtension = null;
        parser.next();
        FileSharingExtension fileSharingExtension = parseFileSharing(parser);
        if (fileSharingExtension != null) voiceMessageExtension = new VoiceMessageExtension(fileSharingExtension);
        return voiceMessageExtension;
    }

    private FileSharingExtension parseFileSharing(XmlPullParser parser) throws Exception {
        FileInfo fileInfo = null;
        FileSources fileSources = null;

        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (FileInfo.FILE_ELEMENT.equals(parser.getName())) {
                        fileInfo = parseFileInfo(parser);
                        parser.next();
                    } else if (FileSources.SOURCES_ELEMENT.equals(parser.getName())) {
                        fileSources = parseFileSources(parser);
                        parser.next();
                    } else {
                        parser.next();
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (FileSharingExtension.FILE_SHARING_ELEMENT.equals(parser.getName())) {
                        break outerloop;
                    } else parser.next();
            }
        }
        if (fileInfo != null && fileSources != null) {
            return new FileSharingExtension(fileInfo, fileSources);
        } else {
            return null;
        }
    }

    private FileInfo parseFileInfo(XmlPullParser parser) throws Exception {
        FileInfo fileInfo = new FileInfo();

        parser.next();
        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    switch (parser.getName()) {
                        case FileInfo.ELEMENT_MEDIA_TYPE:
                            fileInfo.setMediaType(parser.nextText());
                            break;
                        case FileInfo.ELEMENT_NAME:
                            fileInfo.setName(parser.nextText());
                            break;
                        case FileInfo.ELEMENT_DESC:
                            fileInfo.setDesc(parser.nextText());
                            break;
                        case FileInfo.ELEMENT_HEIGHT:
                            fileInfo.setHeight(Integer.parseInt(parser.nextText()));
                            break;
                        case FileInfo.ELEMENT_WIDTH:
                            fileInfo.setWidth(Integer.parseInt(parser.nextText()));
                            break;
                        case FileInfo.ELEMENT_SIZE:
                            fileInfo.setSize(Long.parseLong(parser.nextText()));
                            break;
                        case FileInfo.ELEMENT_DURATION:
                            fileInfo.setDuration(Long.parseLong(parser.nextText()));
                            break;
                        default:
                            parser.next();
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (FileInfo.FILE_ELEMENT.equals(parser.getName())) {
                        break outerloop;
                    } else parser.next();
                    break;
                default:
                    parser.next();
            }
        }
        return fileInfo;
    }

    private FileSources parseFileSources(XmlPullParser parser) throws XmlPullParserException, IOException {
        FileSources fileSources = new FileSources();

        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (FileSources.URI_ELEMENT.equals(parser.getName())) {
                        String uri = parser.nextText();
                        if (uri != null && !uri.isEmpty()) {
                            fileSources.addSource(uri);
                        }
                    } else {
                        parser.next();
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (FileSources.SOURCES_ELEMENT.equals(parser.getName())) {
                        break outerloop;
                    } else parser.next();
                    break;
                default:
                    parser.next();
            }
        }
        return fileSources;
    }

    private GroupMemberExtensionElement parseUser(XmlPullParser parser) throws Exception {
        String id = null;
        String jid = null;
        String nickname = null;
        String role = null;
        String badge = null;
        String present = null;
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
            if (avatar != null)
                user.setAvatarInfo(avatar);
            return user;
        } else return null;
    }

    private MetadataInfo parseAvatar(XmlPullParser parser) {
        try{
            parser.next();
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                if (GroupMemberExtensionElement.ELEMENT_INFO.equals(parser.getName())) {
                    return MetadataProvider.parseInfo(parser);
                }
            }
            return null;
        } catch (Exception e){
            LogManager.exception(ReferencesProvider.class, e);
            return null;
        }
    }


/*
    private RefMedia parseMedia(XmlPullParser parser) throws Exception {
        String uri = null;
        FileSharingExtension file = null;

        parser.next();
        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (FileSharingExtension.FILE_ELEMENT.equals(parser.getName())) {
                        file = parseFileInfo(parser);
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
*/
}
