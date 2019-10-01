package com.xabber.xmpp.avatar;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.pubsub.PubSubManager;

public class Avatar extends IQ {

    private static final String AVATAR_NAMESPACE = "http://jabber.org/protocol/pubsub";
    private static final String AVATAR_ELEMENT_NAME = "pubsub";

    private String sha1hash;
    private String photoHash;

    private boolean metadata = false;

    private Integer bytes;
    private String imageType;
    private short width;
    private short height;

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
        if(metadata) {
            xml.halfOpenElement("data");
            xml.attribute("xmlns", "urn:xmpp:avatar:data");
            xml.rightAngleBracket();
            xml.escape(photoHash);
            xml.closeElement("data");
        }else {
            xml.halfOpenElement("metadata");
            xml.attribute("xmlns", "urn:xmpp:avatar:metadata");
            xml.rightAngleBracket();
            xml.halfOpenElement("info");
            xml.attribute("bytes", bytes);
            xml.attribute("id",sha1hash);
            xml.attribute("type", imageType);
            xml.attribute("height", height);
            xml.attribute("width", width);
            xml.rightAngleBracket();
            xml.closeElement("info");
            xml.closeElement("metadata");
        }
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
    public String getsha1hash(){ return sha1hash;}

    public void setSha1hash(String sha1hash){this.sha1hash=sha1hash;}

    public boolean getMetadata(){return metadata;}

    public void setMetadata(boolean data) {
        this.metadata = data;
    }

    public Integer getBytes(){return bytes;}

    public void setBytes(Integer bytes) {
        this.bytes = bytes;
    }

    public String getImageType(){return imageType;}

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    public short getWidth(){return width;}

    public void setWidth(short width) {
        this.width = width;
    }

    public short getHeight(){return height;}

    public void setHeight(short height) {
        this.height = height;
    }
}
