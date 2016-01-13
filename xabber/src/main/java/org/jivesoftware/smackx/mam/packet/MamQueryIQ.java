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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

/**
 * 
 * @see <a href="http://xmpp.org/extensions/xep-0313.html">XEP-0313: Message
 *      Archive Management</a>
 *
 */
public class MamQueryIQ extends IQ {

    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String NAMESPACE = MamPacket.NAMESPACE;

    private final String queryId;
    private String node;

    public MamQueryIQ(String queryId, String node, DataForm form) {
        super(ELEMENT, NAMESPACE);
        this.queryId = queryId;
        this.node = node;

        if (form != null) {
            FormField field = form.getHiddenFormTypeField();
            if (field == null) {
                throw new IllegalArgumentException(
                        "If a data form is given it must posses a hidden form type field");
            }
            if (!field.getValues().get(0).equals(MamPacket.NAMESPACE)) {
                throw new IllegalArgumentException(
                        "Value of the hidden form type field must be '"
                                + MamPacket.NAMESPACE + "'");
            }
            addExtension(form);
        }
    }

    public MamQueryIQ(DataForm form) {
        this(null, null, form);
    }

    public MamQueryIQ(String queryId) {
        this(queryId, null, null);
    }

    public MamQueryIQ(String queryId, DataForm form) {
        this(queryId, null, form);
    }

    /**
     * 
     */
    public MamQueryIQ() {
      this(null,null,null);
    }

    public String getQueryId() {
        return queryId;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(
            IQChildElementXmlStringBuilder xml) {
        xml.optAttribute("queryid", queryId);
        xml.optAttribute("node", node);
        xml.rightAngleBracket();
        return xml;
    }

    /**
     * @param attributeValue
     */
    public void setNode(String node) {
      this.node = node;
    }

}
