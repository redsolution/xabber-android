package com.xabber.android.data.extension.retract;

import org.jivesoftware.smack.packet.IQ;

public class RetractMessageIQ extends IQ {

    public static final String NAMESPACE = RetractManager.NAMESPACE;
    public static final String ELEMENT = "retract-message";
    public static final String SYMMETRIC_ATTRIBUTE = "symmetric";
    public static final String ID_ATTRIBUTE = "id";
    public static final String BY_ATTRIBUTE = "by";

    private final String by;
    private final String id;
    private final boolean symmetric;

    public String getNamespace(){ return NAMESPACE; }
    public String getElementName(){ return ELEMENT; }

    RetractMessageIQ(String by, String id, boolean symmetric){
        super(ELEMENT, NAMESPACE);
        this.setType(Type.set);
        this.by = by;
        this.id = id;
        this.symmetric = symmetric;
        this.setFrom(by);
    }

    RetractMessageIQ(String by, String archiveJid, String id, boolean symmetric){
        this(by, id, symmetric);
        this.setTo(archiveJid);
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
