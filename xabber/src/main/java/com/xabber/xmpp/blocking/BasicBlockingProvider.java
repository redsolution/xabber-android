package com.xabber.xmpp.blocking;


import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.provider.IQProvider;
import org.jxmpp.jid.impl.JidCreate;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

abstract class BasicBlockingProvider<BlockingIq extends BasicBlockingIq> extends IQProvider<BlockingIq> {

    @Override
    public BlockingIq parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException, SmackException {
        BlockingIq instance = createInstance();

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equalsIgnoreCase(XmlConstants.ITEM)) {

                    final String item = parser.getAttributeValue(null, XmlConstants.ITEM_JID);
                    if (item != null) {
                        instance.addItem(JidCreate.from(item));
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG && parser.getName().equals(instance.getChildElementName())) {
                done = true;
            }
        }

        return instance;
    }


    protected abstract BlockingIq createInstance();


}
