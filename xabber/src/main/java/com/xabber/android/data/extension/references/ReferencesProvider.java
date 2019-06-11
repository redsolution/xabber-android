package com.xabber.android.data.extension.references;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.forward.provider.ForwardedProvider;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

public class ReferencesProvider extends ExtensionElementProvider<ReferenceElement> {

    @Override
    public ReferenceElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        String type = null, beginS = null, endS = null, delS = null;
        List<Forwarded> forwardedMessages = new ArrayList<>();
        List<RefMedia> mediaElements = new ArrayList<>();
        boolean bold = false, italic = false, underline = false, strike = false;

        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (ReferenceElement.ELEMENT.equals(parser.getName())
                            && ReferenceElement.NAMESPACE.equals(parser.getNamespace())) {
                        type = parser.getAttributeValue("", "type");
                        beginS = parser.getAttributeValue("", "begin");
                        endS = parser.getAttributeValue("", "end");
                        delS = parser.getAttributeValue("", "del");
                    }
                    if (Forwarded.ELEMENT.equals(parser.getName())
                            && Forwarded.NAMESPACE.equals(parser.getNamespace())) {
                        Forwarded forwarded = ForwardedProvider.INSTANCE.parse(parser);
                        if (forwarded != null) forwardedMessages.add(forwarded);
                    }
                    if (RefMedia.ELEMENT.equals(parser.getName())) {
                        RefMedia media = parseMedia(parser);
                        if (media != null) mediaElements.add(media);
                    }
                    if (ReferenceElement.ELEMENT_BOLD.equals(parser.getName())) {
                        bold = true;
                    }
                    if (ReferenceElement.ELEMENT_ITALIC.equals(parser.getName())) {
                        italic = true;
                    }
                    if (ReferenceElement.ELEMENT_UNDERLINE.equals(parser.getName())) {
                        underline = true;
                    }
                    if (ReferenceElement.ELEMENT_STRIKE.equals(parser.getName())) {
                        strike = true;
                    }
                    parser.next();
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

        int begin = 0, end = 0, del = 0;
        if (beginS != null && !beginS.isEmpty()) begin = Integer.valueOf(beginS);
        if (endS != null && !endS.isEmpty()) end = Integer.valueOf(endS);
        if (delS != null && !delS.isEmpty()) del = Integer.valueOf(delS);
        return new ReferenceElement(ReferenceElement.Type.valueOf(type), begin, end, del, bold,
                italic, underline, strike, null, forwardedMessages, mediaElements);
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
                        case RefFile.ELEMENT_VOICE:
                            builder.setVoice(Boolean.valueOf(parser.nextText()));
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
}
