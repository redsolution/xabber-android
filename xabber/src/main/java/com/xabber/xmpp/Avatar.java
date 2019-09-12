package com.xabber.xmpp;

import org.jivesoftware.smack.packet.IQ;

public class Avatar extends IQ {

    private static final String AVATAR_NAMESPACE = "http://jabber.org/protocol/pubsub";
    private static final String AVATAR_ELEMENT_NAME = "pubsub";
    private String sha1hash;
    private String photoHash;

    public Avatar() {
        super(AVATAR_ELEMENT_NAME, AVATAR_NAMESPACE);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.halfOpenElement("publish");
        xml.attribute("node","urn:xmpp:avatar:data");
        xml.rightAngleBracket();
        xml.halfOpenElement("item");
        xml.attribute("id",sha1hash);
        xml.rightAngleBracket();
        xml.halfOpenElement("data");
        xml.attribute("xmlns", "urn:xmpp:avatar:data");
        xml.rightAngleBracket();
        xml.escape(photoHash);
        xml.closeElement("xmlns");
        xml.closeElement("item");
        xml.closeElement("publish");

        return xml;
    }

    public String getPhotoHash(){
        return photoHash;
    }
    public void setPhotoHash(String photoHash){
        this.photoHash=photoHash;
    }
}
