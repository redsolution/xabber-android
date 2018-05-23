package com.xabber.android.data.extension.httpfileupload;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.xdata.FormField;

public class ExtendedFormField extends FormField {

    private Media media;

    public ExtendedFormField() {
        super();
    }

    public ExtendedFormField(String variable) {
        super(variable);
    }

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder buf = new XmlStringBuilder(this);
        // Add attributes
        buf.optAttribute("label", getLabel());
        buf.optAttribute("var", getVariable());
        buf.optAttribute("type", getType());
        buf.rightAngleBracket();
        // Add elements
        buf.optElement("desc", getDescription());
        buf.condEmptyElement(isRequired(), "required");
        // Loop through all the values and append them to the string buffer
        for (String value : getValues()) {
            buf.element("value", value);
        }
        // Loop through all the values and append them to the string buffer
        for (Option option : getOptions()) {
            buf.append(option.toXML());
        }

        if (media != null)
            buf.append(media.toXML());

        buf.closeElement(this);
        return buf;
    }

    public static class Uri implements NamedElement {

        public static final String ELEMENT = "uri";

        private String type;
        private String uri;
        private long size;
        private long duration;

        public Uri(String type, String uri) {
            this.type = type;
            this.uri = uri;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public String getType() {
            return type;
        }

        public String getUri() {
            return uri;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            // Add attribute
            xml.optAttribute("type", getType());
            xml.optAttribute("size", String.valueOf(getSize()));
            xml.optAttribute("duration", String.valueOf(getDuration()));
            xml.rightAngleBracket();

            xml.append(getUri());

            xml.closeElement(this);
            return xml;
        }
    }

    public static class Media implements ExtensionElement {

        public static final String ELEMENT = "media";
        public static final String NAMESPACE = "urn:xmpp:media-element";

        private String height;
        private String width;
        private Uri uri;

        public Media(String height, String width, Uri uri) {
            this.height = height;
            this.width = width;
            this.uri = uri;
        }

        public String getHeight() {
            return height;
        }

        public String getWidth() {
            return width;
        }

        public Uri getUri() {
            return uri;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            // Add attribute
            xml.optAttribute("height", getHeight());
            xml.optAttribute("width", getWidth());
            xml.rightAngleBracket();

            // Add element
            if (uri != null)
                xml.append(uri.toXML());

            xml.closeElement(this);
            return xml;
        }
    }

}
