package com.xabber.android.data.extension.references;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.forward.packet.Forwarded;

import java.util.List;

public class ReferenceElement implements ExtensionElement {

    public static final String NAMESPACE = "urn:xmpp:reference:0";
    public static final String ELEMENT = "reference";

    private final Type type;
    private final int begin;
    private final int end;
    private final int del;

    private List<Forwarded> forwarded;
    private List<RefMedia> media;

    public ReferenceElement(Type type, int begin, int end, int del, List<Forwarded> forwarded, List<RefMedia> media) {
        this.type = type;
        this.begin = begin;
        this.end = end;
        this.del = del;
        this.forwarded = forwarded;
        this.media = media;
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
        xml.attribute("type", type);
        xml.attribute("begin", begin);
        xml.attribute("end", end);
        xml.attribute("del", del);
        xml.rightAngleBracket();
        if (forwarded != null && !forwarded.isEmpty()) {
            for (Forwarded forward : forwarded) {
                xml.append(forward.toXML());
            }
        }
        if (media != null && !media.isEmpty()) {
            for (RefMedia media1 : media) {
                xml.append(media1.toXML());
            }
        }
        xml.closeElement(this);
        return xml;
    }

    public Type getType() {
        return type;
    }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }

    public int getDel() {
        return del;
    }

    public List<Forwarded> getForwarded() {
        return forwarded;
    }

    public List<RefMedia> getMedia() {
        return media;
    }

    public enum Type {
        data,
        forward,
        markup,
        mention,
        quote,
        legacy
    }
}