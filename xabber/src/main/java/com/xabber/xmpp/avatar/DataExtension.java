package com.xabber.xmpp.avatar;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.stringencoder.Base64;

public class DataExtension implements ExtensionElement {

    public static final String ELEMENT = "data";
    public static final String NAMESPACE = UserAvatarManager.DATA_NAMESPACE;

    //base64
    private final byte[] data;
    private String photoHash;

    public DataExtension(byte[] data) {
        this.data = data;
    }

    public DataExtension(String encodedString) {
        this.photoHash = StringUtils.requireNotNullOrEmpty(encodedString,
                "Encoded string Must not be null or empty");
        this.data = Base64.decode(encodedString);
    }

    public byte[] getData() {
        return data.clone();
    }

    public String getDataAsString() {
        if (photoHash == null) {
            photoHash = Base64.encodeToString(data);
        }
        return photoHash;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();
        xml.escape(this.getDataAsString());
        xml.closeElement(this);
        return xml;
    }

}