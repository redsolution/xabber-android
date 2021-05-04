package com.xabber.android.data.extension.retract;

import org.jivesoftware.smack.packet.IQ;

public class RetractAllMessagesIQ extends IQ {
    public static final String NAMESPACE = RetractManager.NAMESPACE;
    public static final String ELEMENT = "retract-all";
    public static final String SYMMETRIC_ATTRIBUTE = "symmetric";
    public static final String CONVERSATION_ATTRIBUTE = "conversation";

    private String conversation;
    private boolean symmetric;

    public String getNamespace(){ return NAMESPACE; }
    public String getElementName(){ return ELEMENT; }

    RetractAllMessagesIQ(String conversation, boolean symmetric){
        super(ELEMENT, NAMESPACE);
        this.setType(IQ.Type.set);
        this.conversation = conversation;
        this.symmetric = symmetric;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.attribute(SYMMETRIC_ATTRIBUTE, symmetric);
        xml.attribute(CONVERSATION_ATTRIBUTE, conversation);
        xml.rightAngleBracket();
        return xml;
    }
}
