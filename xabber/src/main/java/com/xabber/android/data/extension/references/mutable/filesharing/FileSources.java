package com.xabber.android.data.extension.references.mutable.filesharing;

import org.jivesoftware.smack.util.XmlStringBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileSources {

    public static final String SOURCES_ELEMENT = "sources";
    public static final String URI_ELEMENT = "uri";

    private List<String> uris;

    public FileSources() {}

    public FileSources(List<String> uris) {
        this.uris = uris;
    }

    public void addSource(String uri) {
        if (uris == null) uris = new ArrayList<String>();
        if (uri != null) uris.add(uri);
    }

    public void addSources(List<String> newUris) {
        if (uris == null) uris = new ArrayList<String>();
        uris.addAll(newUris);
    }

    public List<String> getSources() {
        return uris == null ? null : Collections.unmodifiableList(uris);
    }

    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        if (uris != null && !uris.isEmpty()) {
            xml.openElement(SOURCES_ELEMENT);
            for (String uri : uris) {
                xml.openElement(URI_ELEMENT);
                xml.append(uri);
                xml.closeElement(URI_ELEMENT);
            }
            xml.closeElement(SOURCES_ELEMENT);
        }
        return xml;
    }
}
