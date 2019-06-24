package com.xabber.android.data.extension.references;

import org.jivesoftware.smack.util.XmlStringBuilder;

import java.util.List;

public class Media extends ReferenceElement {
    private final List<RefMedia> media;

    public Media(int begin, int end, List<RefMedia> media) {
        super(begin, end);
        this.media = media;
    }

    @Override
    public Type getType() {
        return Type.media;
    }

    @Override
    public void appendToXML(XmlStringBuilder xml) {
        for (RefMedia item : media) {
            xml.append(item.toXML());
        }
    }

    public List<RefMedia> getMedia() {
        return media;
    }
}
