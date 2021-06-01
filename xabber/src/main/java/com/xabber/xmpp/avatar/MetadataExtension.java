package com.xabber.xmpp.avatar;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;


public class MetadataExtension implements ExtensionElement {

    public static final String ELEMENT = "metadata";
    public static final String NAMESPACE = UserAvatarManager.METADATA_NAMESPACE;

    private final List<MetadataInfo> infos;
    private final List<MetadataPointer> pointers;


    public MetadataExtension(List<MetadataInfo> infos) {
        this(infos, null);
    }


    public MetadataExtension(List<MetadataInfo> infos, List<MetadataPointer> pointers) {
        this.infos = infos;
        this.pointers = pointers;
    }

    public List<MetadataInfo> getInfoElements() {
        return infos == null ? null : Collections.unmodifiableList(infos);
    }

    public List<MetadataPointer> getPointerElements() {
        return (pointers == null) ? null : Collections.unmodifiableList(pointers);
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
        appendInfoElements(xml);
        appendPointerElements(xml);
        closeElement(xml);
        return xml;
    }

    private void appendInfoElements(XmlStringBuilder xml) {
        if (infos != null) {
            xml.rightAngleBracket();

            for (MetadataInfo info : infos) {
                xml.halfOpenElement("info");
                xml.attribute("id", info.getId());
                xml.attribute("bytes", info.getBytes());
                xml.attribute("type", info.getType());
                if (info.getUrl()!=null) xml.optAttribute("url", info.getUrl().toString());
                xml.attribute("height", info.getHeight());
                xml.attribute("width", info.getWidth());
                xml.closeEmptyElement();
            }
        }
    }

    private void appendPointerElements(XmlStringBuilder xml) {
        if (pointers != null) {

            for (MetadataPointer pointer : pointers) {
                xml.openElement("pointer");
                xml.halfOpenElement("x");

                String namespace = pointer.getNamespace();
                if (namespace != null) {
                    xml.xmlnsAttribute(namespace);
                }

                xml.rightAngleBracket();

                Map<String, Object> fields = pointer.getFields();
                if (fields != null) {
                    for (Map.Entry<String, Object> pair : fields.entrySet()) {
                        xml.escapedElement(pair.getKey(), String.valueOf(pair.getValue()));
                    }
                }

                xml.closeElement("x");
                xml.closeElement("pointer");
            }

        }
    }

    private void closeElement(XmlStringBuilder xml) {
        if (infos != null || pointers != null) {
            xml.closeElement(this);
        } else {
            xml.closeEmptyElement();
        }
    }

    public boolean isDisablingPublishing() {
        return getInfoElements().isEmpty();
    }

}