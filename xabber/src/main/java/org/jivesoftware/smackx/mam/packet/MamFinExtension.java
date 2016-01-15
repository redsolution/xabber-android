package org.jivesoftware.smackx.mam.packet;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.rsm.packet.RSMSet;

import java.util.List;

public class MamFinExtension extends AbstractMamExtension {

    public static final String ELEMENT = "fin";

    private RSMSet rsmSet;
    private final Boolean complete;
    private final boolean stable;

    public MamFinExtension(String queryId, RSMSet rsmSet, Boolean complete, boolean stable) {
        super(queryId);

        this.rsmSet = rsmSet;
        this.complete = complete;
        this.stable = stable;
    }

    public RSMSet getRSMSet() {
        return rsmSet;
    }

    public Boolean isComplete() {
        return complete;
    }

    /**
     * the server indicates that the results returned are unstable (e.g. they might later change in sequence or content).
     *
     * @return
     */
    public boolean isStable() {
        return stable;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.halfOpenElement(this);
        xml.optAttribute("queryid", queryId);
        if (complete != null)
            xml.optBooleanAttribute("complete", complete);
        xml.optBooleanAttribute("stable", stable);
        if (rsmSet == null) {
            xml.closeEmptyElement();
        } else {
            xml.rightAngleBracket();
            xml.element(rsmSet);
            xml.closeElement(this);
        }
        return xml;
    }

    public static MamFinExtension from(IQ iq) {
        MamFinExtension fin = iq.getExtension(ELEMENT, MamPacket.NAMESPACE);
//        if (iq.hasExtension(RSMSet.ELEMENT, RSMSet.NAMESPACE) && fin.rsmSet == null) {
//            fin.rsmSet = iq.getExtension(RSMSet.ELEMENT, RSMSet.NAMESPACE);
//        }

        return fin;
    }
}
