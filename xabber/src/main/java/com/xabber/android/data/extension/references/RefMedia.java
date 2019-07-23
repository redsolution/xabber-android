package com.xabber.android.data.extension.references;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class RefMedia {

    public static final String ELEMENT = "media";
    public static final String ELEMENT_URI = "uri";

    private RefFile file;
    private String uri;

    public RefMedia(RefFile file, String uri) {
        this.file = file;
        this.uri = uri;
    }

    public RefFile getFile() {
        return file;
    }

    public String getUri() {
        return uri;
    }

    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.openElement(ELEMENT);
        if (file != null) {
            xml.append(file.toXML());
        }
        if (uri != null) {
            xml.openElement(ELEMENT_URI);
            xml.append(uri);
            xml.closeElement(ELEMENT_URI);
        }
        xml.closeElement(ELEMENT);
        return xml;
    }
}
