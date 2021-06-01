package com.xabber.android.data.extension.references.mutable.filesharing;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class FileInfo {

    public static final String FILE_ELEMENT = "file";

    public static final String ELEMENT_MEDIA_TYPE = "media-type";
    public static final String ELEMENT_NAME = "name";
    public static final String ELEMENT_DESC = "desc";
    public static final String ELEMENT_HEIGHT = "height";
    public static final String ELEMENT_WIDTH = "width";
    public static final String ELEMENT_SIZE = "size";
    public static final String ELEMENT_DURATION = "duration";

    private String mediaType;
    private String name;
    private String desc;
    private int height;
    private int width;
    private long size;
    private long duration;

    public FileInfo() {}

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

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.openElement(FILE_ELEMENT);
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
        xml.closeElement(FILE_ELEMENT);
        return xml;
    }
}
