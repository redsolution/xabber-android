package com.xabber.android.data.extension.retract;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;

public class ReplaceMessageIQ extends IQ {
    public static final String NAMESPACE = RetractManager.NAMESPACE;
    public static final String ELEMENT = "replace";
    public static final String ID_ATTRIBUTE = "id";
    public static final String BY_ATTRIBUTE = "by";

    private String id, by;
    private Message message;

    ReplaceMessageIQ(String id, String by, Message message){
        super(ELEMENT, NAMESPACE);
        this.id = id;
        this.by = by;
        this.message = message;
        this.setType(Type.set);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.attribute(BY_ATTRIBUTE, by);
        xml.attribute(ID_ATTRIBUTE, id);
        xml.rightAngleBracket();
        xml.append(message.toXML());
        return xml;
    }
}
