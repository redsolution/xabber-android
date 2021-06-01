package com.xabber.android.data.extension.rrr;

import org.jivesoftware.smack.packet.IQ;

public class RetractMessageIQ extends IQ {

    public static final String NAMESPACE = RrrManager.NAMESPACE;
    public static final String ELEMENT = "retract-message";
    public static final String SYMMETRIC_ATTRIBUTE = "symmetric";
    public static final String ID_ATTRIBUTE = "id";
    public static final String BY_ATTRIBUTE = "by";

    private String by;
    private String id;
    private boolean symmetric;

    public String getNamespace(){ return NAMESPACE; }
    public String getElementName(){ return ELEMENT; }

    RetractMessageIQ(String by, String id, boolean symmetric){
        super(ELEMENT, NAMESPACE);
        this.setType(Type.set);
        this.by = by;
        this.id = id;
        this.symmetric = symmetric;
        this.setFrom(this.by);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.attribute(SYMMETRIC_ATTRIBUTE, symmetric);
        xml.attribute(BY_ATTRIBUTE, by);
        xml.attribute(ID_ATTRIBUTE, id);
        xml.rightAngleBracket();
        return xml;
    }
}
