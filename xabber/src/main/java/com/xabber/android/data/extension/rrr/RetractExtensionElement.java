package com.xabber.android.data.extension.rrr;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class RetractExtensionElement extends ExtensionElement {

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

    RetractExtensionElement(String by, String id, boolean symmetric){
        this.by = by;
        this.id = id;
        this.symmetric = symmetric;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xmlStringBuilder = new XmlStringBuilder();
        //xmlStringBuilder.attribute()
        return null;
    }
}
