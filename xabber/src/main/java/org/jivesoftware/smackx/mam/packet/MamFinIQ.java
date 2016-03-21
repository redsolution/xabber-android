package org.jivesoftware.smackx.mam.packet;


import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.rsm.packet.RSMSet;

public class MamFinIQ extends IQ {

    public static final String ELEMENT = "fin";
    public static final String NAMESPACE = MamPacket.NAMESPACE;

    private RSMSet rsmSet;
    private Boolean complete;

    public MamFinIQ() {
        super(ELEMENT, NAMESPACE);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(
            IQChildElementXmlStringBuilder xml) {
        // TODO Auto-generated method stub
        return null;
    }

    public RSMSet getRsmSet() {
        return rsmSet;
    }

    public void setRsmSet(RSMSet rsmSet) {
        this.rsmSet = rsmSet;
    }

    public Boolean getComplete() {
        return complete;
    }

    public void setComplete(Boolean complete) {
        this.complete = complete;
    }
}
