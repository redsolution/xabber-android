package com.xabber.android.data.extension.references;

import org.jivesoftware.smack.util.XmlStringBuilder;

import java.util.List;

public class Voice extends ReferenceElement {
    private final List<RefMedia> voice;

    public Voice(int begin, int end, List<RefMedia> voice) {
        super(begin, end);
        this.voice = voice;
    }

    @Override
    public Type getType() {
        return Type.voice;
    }

    @Override
    public void appendToXML(XmlStringBuilder xml) {
        for (RefMedia item : voice) {
            xml.append(item.toXML());
        }
    }

    public List<RefMedia> getVoice() {
        return voice;
    }

}
