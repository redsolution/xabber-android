package com.xabber.android.data.extension.reliablemessagedelivery;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class RetryReceiptRequestElement extends ReceiptRequestElement {

    public static final String ATTRIBUTE_RETRY = "retry";

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xmlStringBuilder = new XmlStringBuilder(this);
        xmlStringBuilder.attribute(ATTRIBUTE_RETRY, "true");
        xmlStringBuilder.closeEmptyElement();
        return xmlStringBuilder;
    }
}
