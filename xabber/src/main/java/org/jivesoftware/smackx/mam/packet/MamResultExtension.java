package org.jivesoftware.smackx.mam.packet;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.forward.packet.Forwarded;

public class MamResultExtension extends AbstractMamExtension {

    public static final String ELEMENT = "result";

    private final String id;
    private final Forwarded forwarded;

    public MamResultExtension(String queryId, String id, Forwarded forwarded) {
        super(queryId);
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("id must not be null or empty");
        }
        if (forwarded == null) {
            throw new IllegalArgumentException("forwarded must no be null");
        }
        this.id = id;
        this.forwarded = forwarded;
    }

    public String getId() {
        return id;
    }

    public Forwarded getForwarded() {
        return forwarded;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.halfOpenElement(this);
        xml.optAttribute("queryid", queryId);
        xml.optAttribute("id", id);
        if (forwarded == null) {
            xml.closeEmptyElement();
        } else {
            xml.rightAngleBracket();
            xml.element(forwarded);
            xml.closeElement(this);
        }
        return xml;
    }

    public static MamResultExtension from(Message message) {
        return message.getExtension(ELEMENT, MamPacket.NAMESPACE);
    }

}
