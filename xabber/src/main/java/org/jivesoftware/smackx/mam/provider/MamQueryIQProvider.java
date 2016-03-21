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
package org.jivesoftware.smackx.mam.provider;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.mam.packet.MamQueryIQ;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jivesoftware.smackx.xdata.provider.DataFormProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 *
 * @see <a href="http://xmpp.org/extensions/xep-0313.html">XEP-0313: Message Archive Management</a>
 *
 */
public class MamQueryIQProvider extends IQProvider<MamQueryIQ> {

    /* (non-Javadoc)
     * @see org.jivesoftware.smack.provider.Provider#parse(org.xmlpull.v1.XmlPullParser, int)
     */
    @Override
    public MamQueryIQ parse(XmlPullParser parser, int initialDepth) throws IOException, XmlPullParserException, SmackException {
        boolean done = false;

        MamQueryIQ stanza = new MamQueryIQ(parser.getAttributeValue("", "queryid"));
        if (parser.getName().equals("query")) {
            stanza.setNode(parser.getAttributeValue("", "node"));
        }

        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals(DataForm.ELEMENT) && parser.getNamespace().equals(DataForm.NAMESPACE)) {

                    DataFormProvider prov = new DataFormProvider();
                    stanza.addExtension(prov.parse(parser));
                }
            } else {
                if (eventType == XmlPullParser.END_TAG && parser.getDepth() == initialDepth) {
                    done = true;
                }
            }
        }

        return stanza;
    }

}
