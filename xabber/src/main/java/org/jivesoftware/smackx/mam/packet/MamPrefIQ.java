/**
 * Copyright � 2015 Florian Schmaus
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.mam.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.mam.Behaviour;


/**
 * http://xmpp.org/extensions/xep-0313.html#prefs
 */
public class MamPrefIQ extends IQ {
    public static final String ELEMENT = "prefs";
    public static final String NAMESPACE = MamPacket.NAMESPACE;
    public static final String ATTR_DEFAULT_BEHAVIOUR = "default";
    public static final String ELEM_ALWAYS = "always";
    public static final String ELEM_NEVER = "never";

    private Behaviour behaviour;

    public MamPrefIQ() {
        super(ELEMENT, NAMESPACE);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(
            IQChildElementXmlStringBuilder xml) {
        xml.optAttribute(ATTR_DEFAULT_BEHAVIOUR, behaviour);
        xml.rightAngleBracket();

        if (getType() == Type.set) {
            xml.emptyElement(ELEM_ALWAYS);
            xml.emptyElement(ELEM_NEVER);
        }

        return xml;
    }

    public Behaviour getBehaviour() {
        return behaviour;
    }

    public void setBehaviour(Behaviour behaviour) {
        this.behaviour = behaviour;
    }
}
