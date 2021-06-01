package com.xabber.xmpp.avatar;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * User Avatar metadata provider class.
 *
 * @see <a href="http://xmpp.org/extensions/xep-0084.html">XEP-0084: User
 *      Avatar</a>
 */
public class MetadataProvider extends ExtensionElementProvider<MetadataExtension> {

    @Override
    public MetadataExtension parse(XmlPullParser parser, int initialDepth) throws IOException, XmlPullParserException, XmlPullParserException {
        List<MetadataInfo> metadataInfos = null;
        List<MetadataPointer> pointers = null;

        while (true) {
            int eventType = parser.nextTag();

            if (eventType == XmlPullParser.START_TAG) {

                if (parser.getName().equals("info")) {
                    if (metadataInfos == null) {
                        metadataInfos = new ArrayList<>();
                    }

                    MetadataInfo info = parseInfo(parser);
                    if (info.getId() != null) {
                        metadataInfos.add(info);
                    }
                }

                if (parser.getName().equals("pointer")) {
                    if (pointers == null) {
                        pointers = new ArrayList<>();
                    }

                    pointers.add(parsePointer(parser));
                }

            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getDepth() == initialDepth) {
                    break;
                }
            }
        }

        return new MetadataExtension(metadataInfos, pointers);
    }

    private MetadataInfo parseInfo(XmlPullParser parser) throws XmlPullParserException {
        String id;
        URL url = null;
        long bytes = 0;
        String type;
        int pixelsHeight = 0;
        int pixelsWidth = 0;

        id = parser.getAttributeValue("", "id");
        type = parser.getAttributeValue("", "type");
        String urlString = parser.getAttributeValue("", "url");
        if (urlString != null && !urlString.isEmpty()) {
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                throw new XmlPullParserException("Cannot parse URL '" + urlString + "'");
            }
        }

        String bytesString = parser.getAttributeValue("", "bytes");
        if (bytesString != null) {
            bytes = Long.parseLong(bytesString);
        }

        String widthString = parser.getAttributeValue("", "width");
        if (widthString != null) {
            pixelsWidth = Integer.parseInt(widthString);
        }

        String heightString = parser.getAttributeValue("", "height");
        if (heightString != null) {
            pixelsHeight = Integer.parseInt(heightString);
        }

        return new MetadataInfo(id, url, bytes, type, pixelsHeight, pixelsWidth);

    }

    private MetadataPointer parsePointer(XmlPullParser parser) throws XmlPullParserException, IOException {
        int pointerDepth = parser.getDepth();
        String namespace = null;
        HashMap<String, Object> fields = null;

        while (true) {
            int eventType2 = parser.nextTag();

            if (eventType2 == XmlPullParser.START_TAG) {
                if (parser.getName().equals("x")) {
                    namespace = parser.getNamespace();
                } else {
                    if (fields == null) {
                        fields = new HashMap<>();
                    }

                    String name = parser.getName();
                    Object value = parser.nextText();
                    fields.put(name, value);
                }
            } else if (eventType2 == XmlPullParser.END_TAG) {
                if (parser.getDepth() == pointerDepth) {
                    break;
                }
            }
        }

        return new MetadataPointer(namespace, fields);
    }

}














/*
package com.xabber.xmpp.avatar;

import com.xabber.xmpp.ProviderUtils;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;

public class MetadataProvider extends ExtensionElementProvider<Metadata> {
    Metadata metadata = new Metadata();

    @Override
    public Metadata parse(XmlPullParser parser, int initialDepth) throws Exception {

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("info")) {
                    for (int i = 0;i<parser.getAttributeCount();i++){
                        switch (parser.getAttributeName(i)){
                            case "bytes":
                                metadata.setBytes(Integer.parseInt(parser.getAttributeValue(i)));
                            case "id":
                                metadata.setShaHash(parser.getAttributeValue(i));
                            case "height":
                                metadata.setHeight(Short.parseShort(parser.getAttributeValue(i)));
                            case "type":
                                metadata.setType(parser.getAttributeValue(i));
                            case "width":
                                metadata.setWidth(Short.parseShort(parser.getAttributeValue(i)));
                        }
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG && parser.getName().equals("info")) {
                done = true;
            }
        }
        return metadata;
    }
}
*/
