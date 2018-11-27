package com.xabber.android.data.extension.privatestorage;

import org.jivesoftware.smackx.iqprivate.packet.PrivateData;
import org.jivesoftware.smackx.iqprivate.provider.PrivateDataProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class XabberOptionsPrivateData implements PrivateData {

    private String elementName;
    private String namespace;
    private Map<String, String> map;

    /**
     * Creates a new generic private data object.
     *
     * @param elementName the name of the element of the XML sub-document.
     * @param namespace the namespace of the element.
     */
    public XabberOptionsPrivateData(String elementName, String namespace) {
        this.elementName = elementName;
        this.namespace = namespace;
    }

    /**
     * Returns the XML element name of the private data sub-packet root element.
     *
     * @return the XML element name of the stanza(/packet) extension.
     */
    @Override
    public String getElementName() {
        return elementName;
    }

    /**
     * Returns the XML namespace of the private data sub-packet root element.
     *
     * @return the XML namespace of the stanza(/packet) extension.
     */
    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append('<').append(elementName).append(" xmlns=\"").append(namespace).append("\">");
        for (String name : getNames()) {
            String value = getValue(name);
            buf.append("<option type=\'").append(name).append("\'>");
            buf.append(value);
            buf.append("</option>");
        }
        buf.append("</").append(elementName).append('>');
        return buf.toString();
    }

    /**
     * Returns a Set of the names that can be used to get
     * values of the private data.
     *
     * @return a Set of the names.
     */
    public synchronized Set<String> getNames() {
        if (map == null) {
            return Collections.<String>emptySet();
        }
        return Collections.unmodifiableSet(map.keySet());
    }

    /**
     * Returns a value given a name.
     *
     * @param name the name.
     * @return the value.
     */
    public synchronized String getValue(String name) {
        if (map == null) {
            return null;
        }
        return map.get(name);
    }

    /**
     * Sets a value given the name.
     *
     * @param name the name.
     * @param value the value.
     */
    public synchronized void setValue(String name, String value) {
        if (map == null) {
            map = new HashMap<String,String>();
        }
        map.put(name, value);
    }

    public static class Provider implements PrivateDataProvider {

        public Provider() {
            super();
        }

        @Override
        public PrivateData parsePrivateData(XmlPullParser parser) throws XmlPullParserException, IOException {
            String elementName = parser.getName();
            String namespace = parser.getNamespace();

            XabberOptionsPrivateData data = new XabberOptionsPrivateData(elementName, namespace);
            boolean finished = false;
            int position = 0;
            while (!finished) {
                int event = parser.next();
                if (event == XmlPullParser.START_TAG) {

                    // parse attributes of tag
                    String type = "type" + position;
                    if (parser.getAttributeCount() > 0) {
                        type = parser.getAttributeValue("", "type");
                    }

                    // If an empty element, set the value with the empty string.
                    if (parser.isEmptyElementTag()) {
                        data.setValue(type,"");
                    }

                    // Otherwise, get the the element text.
                    else {
                        event = parser.next();
                        if (event == XmlPullParser.TEXT) {
                            String value = parser.getText();
                            data.setValue(type, value);
                        }
                    }
                }
                else if (event == XmlPullParser.END_TAG) {
                    if (parser.getName().equals(elementName)) {
                        finished = true;
                    }
                }
                position++;
            }

            return data;
        }
    }

}
