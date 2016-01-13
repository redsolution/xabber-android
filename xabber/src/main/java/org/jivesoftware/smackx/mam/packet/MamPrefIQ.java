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


/**
 * 
 * @see <a href="http://xmpp.org/extensions/xep-0313.html">XEP-0313: Message Archive Management</a>
 *
 */
public class MamPrefIQ extends IQ {


    public static final String ELEMENT = "prefs";
    public static final String NAMESPACE = MamPacket.NAMESPACE;

    public MamPrefIQ() {
        super(ELEMENT, NAMESPACE);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(
            IQChildElementXmlStringBuilder xml) {
        // TODO Auto-generated method stub
        return null;
    }
}
