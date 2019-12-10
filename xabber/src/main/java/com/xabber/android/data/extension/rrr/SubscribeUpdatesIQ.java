package com.xabber.android.data.extension.rrr;

import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class SubscribeUpdatesIQ extends IQ {

    public static final String NAMESPACE = "http://xabber.com/protocol/rewrite";
    public static final String ATTRIBUTE_FROM = "from";
    public static final String ELEMENT = "activate";

    private String from;

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(final IQChildElementXmlStringBuilder xml) {
        xml.attribute(ATTRIBUTE_FROM, from);
        xml.rightAngleBracket();
        xml.element(new Element() {
            @Override
            public XmlStringBuilder toXML() {
                //XmlStringBuilder xmlStringBuilder = new XmlStringBuilder(ELEMENT, NAMESPACE);
                //TODO something here
                return null;
            }
        });
        return xml;
    }

    protected SubscribeUpdatesIQ(String from ){
        super(ELEMENT, NAMESPACE);
        this.setType(Type.set);
        this.from = from;
    }
}
