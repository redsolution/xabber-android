package com.xabber.android.data.extension.chat_markers;

import com.xabber.xmpp.sid.StanzaIdElement;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.XmlStringBuilder;

import java.util.ArrayList;
import java.util.List;


public class ChatMarkersElements {

    public static final String NAMESPACE = "urn:xmpp:chat-markers:0";


    /**
     * Markable extension class.
     *
     * @see <a href="http://xmpp.org/extensions/xep-0333.html">XEP-0333: Chat
     *      Markers</a>
     * @author Fernando Ramirez
     *
     */
    public static final class MarkableExtension implements ExtensionElement {

        /**
         * markable element.
         */
        public static final String ELEMENT = ChatMarkersState.markable.toString();

        public MarkableExtension() {
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
            xml.closeEmptyElement();
            return xml;
        }

        public static MarkableExtension from(Message message) {
            return (MarkableExtension) message.getExtension(ELEMENT, NAMESPACE);
        }
    }

    protected abstract static class ChatMarkerExtensionWithId implements ExtensionElement {

        protected final String id;
        protected ArrayList<ExtensionElement> stanzaIdExtensions = new ArrayList<>();

        protected ChatMarkerExtensionWithId(String id) {
            this.id = id;
        }

        /**
         * Get the id.
         *
         * @return the id
         */
        public final String getId() {
            return id;
        }

        public void setStanzaIdExtensions(List<ExtensionElement> stanzaIdExtensions) {
            this.stanzaIdExtensions = new ArrayList<>(stanzaIdExtensions);
        }

        public void addStanzaIdExtension(StanzaIdElement stanzaIdElement) {
            if (stanzaIdElement == null || stanzaIdElement.getBy() == null || stanzaIdElement.getId() == null)
                return;
            stanzaIdExtensions.add(stanzaIdElement);
        }

        public ArrayList<String> getStanzaId() {
            ArrayList<String> ids = new ArrayList<>();
            for (ExtensionElement idElement : stanzaIdExtensions) {
                ids.add(((StanzaIdElement)idElement).getId());
            }
            return ids;
        }

        @Override
        public final XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.optAttribute("id", id);
            if (stanzaIdExtensions.isEmpty())
                xml.closeEmptyElement();
            else {
                xml.rightAngleBracket();
                for (ExtensionElement id : stanzaIdExtensions) {
                    xml.optElement(id);
                }
                xml.closeElement(getElementName());
            }
            return xml;
        }
    }

    /**
     * Received extension class.
     *
     * @see <a href="http://xmpp.org/extensions/xep-0333.html">XEP-0333: Chat
     *      Markers</a>
     * @author Fernando Ramirez
     *
     */
    public static class ReceivedExtension extends ChatMarkerExtensionWithId {

        /**
         * received element.
         */
        public static final String ELEMENT = ChatMarkersState.received.toString();

        public ReceivedExtension(String id) {
            super(id);
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        public static ReceivedExtension from(Message message) {
            return (ReceivedExtension) message.getExtension(ELEMENT, NAMESPACE);
        }
    }

    /**
     * Displayed extension class.
     *
     * @see <a href="http://xmpp.org/extensions/xep-0333.html">XEP-0333: Chat
     *      Markers</a>
     * @author Fernando Ramirez
     *
     */
    public static class DisplayedExtension extends ChatMarkerExtensionWithId {

        /**
         * displayed element.
         */
        public static final String ELEMENT = ChatMarkersState.displayed.toString();

        public DisplayedExtension(String id) {
            super(id);
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        public static DisplayedExtension from(Message message) {
            return (DisplayedExtension) message.getExtension(ELEMENT, NAMESPACE);
        }
    }

    /**
     * Acknowledged extension class.
     *
     * @see <a href="http://xmpp.org/extensions/xep-0333.html">XEP-0333: Chat
     *      Markers</a>
     * @author Fernando Ramirez
     *
     */
    public static class AcknowledgedExtension extends ChatMarkerExtensionWithId {

        /**
         * acknowledged element.
         */
        public static final String ELEMENT = ChatMarkersState.acknowledged.toString();

        public AcknowledgedExtension(String id) {
            super(id);
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        public static AcknowledgedExtension from(Message message) {
            return (AcknowledgedExtension) message.getExtension(ELEMENT, NAMESPACE);
        }
    }

}
