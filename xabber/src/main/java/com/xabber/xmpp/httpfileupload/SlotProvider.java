package com.xabber.xmpp.httpfileupload;

import com.xabber.xmpp.ProviderUtils;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * http://xmpp.org/extensions/xep-0363.html
 */
public class SlotProvider  extends IQProvider<Slot> {
    public SlotProvider() {
    }

    @Override
    public Slot parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException, SmackException {
        Slot slot = new Slot();

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals(Slot.GET)) {
                    slot.setGetUrl(ProviderUtils.parseText(parser));
                }
                if (parser.getName().equals(Slot.PUT)) {
                    slot.setPutUrl(ProviderUtils.parseText(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG && parser.getName().equals(Slot.ELEMENT_NAME)) {
                done = true;
            }
        }

        return slot;
    }
}
