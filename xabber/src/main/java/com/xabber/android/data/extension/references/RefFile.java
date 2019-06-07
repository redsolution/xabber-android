package com.xabber.android.data.extension.references;

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
