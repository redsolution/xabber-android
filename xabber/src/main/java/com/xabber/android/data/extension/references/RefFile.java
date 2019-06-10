package com.xabber.android.data.extension.references;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class RefFile {

    public static final String ELEMENT = "file";

    public static final String ELEMENT_MEDIA_TYPE = "media-type";
    public static final String ELEMENT_NAME = "name";
    public static final String ELEMENT_DESC = "desc";
    public static final String ELEMENT_HEIGHT = "height";
    public static final String ELEMENT_WIDTH = "width";
    public static final String ELEMENT_SIZE = "size";
    public static final String ELEMENT_DURATION = "duration";
    public static final String ELEMENT_VOICE = "voice";

    private String mediaType;
    private String name;
    private String desc;
    private int height;
    private int width;
    private long size;
    private long duration;
    private boolean voice;

    public String getMediaType() {
        return mediaType;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public long getSize() {
        return size;
    }

    public long getDuration() {
        return duration;
    }

    public boolean isVoice() {
        return voice;
    }

    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.openElement(ELEMENT);
        if (getMediaType() != null && !getMediaType().isEmpty()) {
            xml.openElement(ELEMENT_MEDIA_TYPE);
            xml.append(getMediaType());
            xml.closeElement(ELEMENT_MEDIA_TYPE);
        }
        if (getName() != null && !getName().isEmpty()) {
            xml.openElement(ELEMENT_NAME);
            xml.append(getName());
            xml.closeElement(ELEMENT_NAME);
        }
        if (getHeight() > 0) {
            xml.openElement(ELEMENT_HEIGHT);
            xml.append(String.valueOf(getHeight()));
            xml.closeElement(ELEMENT_HEIGHT);
        }
        if (getWidth() > 0) {
            xml.openElement(ELEMENT_WIDTH);
            xml.append(String.valueOf(getWidth()));
            xml.closeElement(ELEMENT_WIDTH);
        }
        if (getSize() > 0) {
            xml.openElement(ELEMENT_SIZE);
            xml.append(String.valueOf(getSize()));
            xml.closeElement(ELEMENT_SIZE);
        }
        if (getDesc() != null && !getDesc().isEmpty()) {
            xml.openElement(ELEMENT_DESC);
            xml.append(getDesc());
            xml.closeElement(ELEMENT_DESC);
        }
        if (getDuration() > 0) {
            xml.openElement(ELEMENT_DURATION);
            xml.append(String.valueOf(getDuration()));
            xml.closeElement(ELEMENT_DURATION);
        }
        if (isVoice()) {
            xml.openElement(ELEMENT_VOICE);
            xml.append(String.valueOf(isVoice()));
            xml.closeElement(ELEMENT_VOICE);
        }
        xml.closeElement(ELEMENT);
        return xml;
    }

    public static Builder newBuilder() {
        return new RefFile().new Builder();
    }

    public class Builder {
        private Builder() {}

        public RefFile build() {
            return RefFile.this;
        }

        public Builder setMediaType(String mediaType) {
            RefFile.this.mediaType = mediaType;
            return this;
        }

        public Builder setName(String name) {
            RefFile.this.name = name;
            return this;
        }

        public Builder setDesc(String desc) {
            RefFile.this.desc = desc;
            return this;
        }

        public Builder setHeight(int height) {
            RefFile.this.height = height;
            return this;
        }

        public Builder setWidth(int width) {
            RefFile.this.width = width;
            return this;
        }

        public Builder setSize(long size) {
            RefFile.this.size = size;
            return this;
        }

        public Builder setDuration(long duration) {
            RefFile.this.duration = duration;
            return this;
        }

        public Builder setVoice(boolean voice) {
            RefFile.this.voice = voice;
            return this;
        }
    }

}
