package com.xabber.android.data.extension.references.mutable;

import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.forward.packet.Forwarded;

import java.util.List;

public class Forward extends Mutable {

    private final List<Forwarded> forwarded;

    public Forward(int begin, int end, List<Forwarded> forwarded) {
        super(begin, end);
        this.forwarded = forwarded;
    }

    @Override
    public void appendToXML(XmlStringBuilder xml) {
        for (Forwarded forward : forwarded) {
            xml.append(forward.toXML());
        }
    }

    public List<Forwarded> getForwarded() {
        return forwarded;
    }
}
