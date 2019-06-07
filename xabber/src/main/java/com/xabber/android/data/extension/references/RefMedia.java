package com.xabber.android.data.extension.references;

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

}
