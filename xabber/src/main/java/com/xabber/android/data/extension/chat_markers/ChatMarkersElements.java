package com.xabber.android.data.extension.chat_markers;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;


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

        public static final MarkableExtension INSTANCE = new MarkableExtension();

        /**
         * markable element.
         */
        public static final String ELEMENT = ChatMarkersState.markable.toString();

        private MarkableExtension() {
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

        protected ChatMarkerExtensionWithId(String id) {
            this.id = StringUtils.requireNotNullOrEmpty(id, "Message ID must not be null");
        }

        /**
         * Get the id.
         *
         * @return the id
         */
        public final String getId() {
            return id;
        }

        @Override
        public final XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.attribute("id", id);
            xml.closeEmptyElement();
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
