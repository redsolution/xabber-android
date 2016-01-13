/**
 *
 * Copyright © 2015 Florian Schmaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.mam.packet;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.rsm.packet.RSMSet;


/**
 * 
 * @see <a href="http://xmpp.org/extensions/xep-0313.html">XEP-0313: Message Archive Management</a>
 *
 */
public class MamPacket {

    public static final String NAMESPACE = "urn:xmpp:mam:0";

    public static abstract class AbstractMamExtension implements ExtensionElement {
        public final String queryId;

        protected AbstractMamExtension(String queryId) {
            this.queryId = queryId;
        }

        public final String getQueryId() {
            return queryId;
        }
        

        @Override
        public final String getNamespace() {
            return NAMESPACE;
        }

    }

    public static class MamFinExtension extends AbstractMamExtension {

        public static final String ELEMENT = "fin";

        private RSMSet rsmSet;
        private final Boolean complete;
        private final boolean stable;

        public MamFinExtension(String queryId, RSMSet rsmSet, Boolean complete, boolean stable) {
            super(queryId);
//            if (rsmSet == null) {
//                throw new IllegalArgumentException("rsmSet must not be null");
//            }
            this.rsmSet = rsmSet;
            this.complete = complete;
            this.stable = stable;
        }

        public RSMSet getRSMSet() {
            return rsmSet;
        }

        public Boolean isComplete() {
            return complete;
        }

        /**
         * the server indicates that the results returned are unstable (e.g. they might later change in sequence or content).
         * @return
         */
        public boolean isStable() {
            return stable;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(this);
            xml.optAttribute("queryid", queryId);
            if (complete != null )
              xml.optBooleanAttribute("complete", complete);
            xml.optBooleanAttribute("stable", stable);
            if (rsmSet == null) {
                xml.closeEmptyElement();
            } else {
                xml.rightAngleBracket();
                xml.element(rsmSet);
                xml.closeElement(this);
            }
            return xml;
        }

        public static MamFinExtension from(Message message) {
          MamFinExtension fin = message.getExtension(ELEMENT, NAMESPACE);
          if (message.hasExtension(RSMSet.ELEMENT, RSMSet.NAMESPACE) && fin.rsmSet == null) {
            fin.rsmSet = message.getExtension(RSMSet.ELEMENT, RSMSet.NAMESPACE);
          }
          
          return fin;
        }
    }

    public static class MamResultExtension extends AbstractMamExtension {

        public static final String ELEMENT = "result";

        private final String id;
        private final Forwarded forwarded;

        public MamResultExtension(String queryId, String id, Forwarded forwarded) {
            super(queryId);
            if (StringUtils.isEmpty(id)) {
                throw new IllegalArgumentException("id must not be null or empty");
            }
            if (forwarded == null) {
                throw new IllegalArgumentException("forwarded must no be null");
            }
            this.id = id;
            this.forwarded = forwarded;
        }

        public String getId() {
            return id;
        }

        public Forwarded getForwarded() {
            return forwarded;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public CharSequence toXML() {
          XmlStringBuilder xml = new XmlStringBuilder();
          xml.halfOpenElement(this);
          xml.optAttribute("queryid", queryId);
          xml.optAttribute("id", id);
          if (forwarded == null) {
              xml.closeEmptyElement();
          } else {
              xml.rightAngleBracket();
              xml.element(forwarded);
              xml.closeElement(this);
          }
          return xml;
        }

        public static MamResultExtension from(Message message) {
            return message.getExtension(ELEMENT, NAMESPACE);
        }

    }
}
