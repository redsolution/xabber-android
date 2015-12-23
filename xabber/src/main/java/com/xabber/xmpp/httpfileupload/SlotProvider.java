package com.xabber.xmpp.httpfileupload;

import com.xabber.xmpp.AbstractIQProvider;
import com.xabber.xmpp.ProviderUtils;

import org.jivesoftware.smack.SmackException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * http://xmpp.org/extensions/xep-0363.html
 */
public class SlotProvider  extends AbstractIQProvider<Slot> {
    public SlotProvider() {
    }

    @Override
    protected Slot createInstance(XmlPullParser parser) {
        return new Slot();
    }

    @Override
    protected boolean parseInner(XmlPullParser parser, Slot instance) throws XmlPullParserException, IOException, SmackException {
        if (super.parseInner(parser, instance)) {
            return true;
        }

        if (parser.getName().equalsIgnoreCase(Slot.GET)) {
            instance.setGetUrl(ProviderUtils.parseText(parser));
        }
        if (parser.getName().equalsIgnoreCase(Slot.PUT)) {
            instance.setPutUrl(ProviderUtils.parseText(parser));
        }

        return true;
    }
}
