/**
 * Copyright © 2015 Florian Schmaus
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
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.forward.provider.ForwardedProvider;
import org.jivesoftware.smackx.mam.packet.MamResultExtension;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 *
 * @see <a href="http://xmpp.org/extensions/xep-0313.html">XEP-0313: Message Archive Management</a>
 *
 */
public class MamResultProvider extends ExtensionElementProvider<MamResultExtension> {

    /* (non-Javadoc)
     * @see org.jivesoftware.smack.provider.Provider#parse(org.xmlpull.v1.XmlPullParser, int)
     */
    @Override
    public MamResultExtension parse(XmlPullParser parser, int initialDepth) throws IOException, XmlPullParserException, SmackException {
        boolean done = false;

        String queryId = parser.getAttributeValue("", "queryid");
        String id = parser.getAttributeValue("", "id");
        Forwarded forwarded = null;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("forwarded")) {
                    forwarded = new ForwardedProvider().parse(parser);
                }
            } else {
                if (eventType == XmlPullParser.END_TAG && parser.getDepth() == initialDepth) {
                    done = true;
                }
            }
        }
        MamResultExtension stanza = new MamResultExtension(queryId, id, forwarded);

        return stanza;
    }
}
