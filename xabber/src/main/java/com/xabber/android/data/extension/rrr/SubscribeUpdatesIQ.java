package com.xabber.android.data.extension.rrr;

import com.xabber.android.data.entity.AccountJid;

import org.jivesoftware.smack.packet.IQ;

public class SubscribeUpdatesIQ extends IQ {

    public static final String NAMESPACE = "http://xabber.com/protocol/rewrite";
    public static final String ELEMENT = "activate";
    private String from;

    protected SubscribeUpdatesIQ(AccountJid from) {
        super(ELEMENT, NAMESPACE);
        this.setType(Type.set);
        this.from = from.toString();
        this.setFrom(this.from);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(final IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        return xml;
    }
}
