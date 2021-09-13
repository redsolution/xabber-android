package com.xabber.xmpp.xtoken;

import com.xabber.xmpp.xtoken.IncomingNewXTokenIQ;

import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import java.util.concurrent.TimeUnit;

public class XTokenProvider extends IQProvider<IncomingNewXTokenIQ> {

    @Override
    public IncomingNewXTokenIQ parse(XmlPullParser parser, int initialDepth) throws Exception {

        String token = null;
        String tokenUID = null;
        long expire = 0;

        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (IncomingNewXTokenIQ.ELEMENT.equals(parser.getName())
                            && IncomingNewXTokenIQ.NAMESPACE.equals(parser.getNamespace())) {
                        parser.next();
                    } else if (IncomingNewXTokenIQ.ELEMENT_TOKEN.equals(parser.getName())) {
                        token = parser.nextText();
                    } else if (IncomingNewXTokenIQ.ELEMENT_TOKEN_UID.equals(parser.getName())) {
                        tokenUID = parser.nextText();
                    } else if (IncomingNewXTokenIQ.ELEMENT_EXPIRE.equals(parser.getName())) {
                        expire = Long.valueOf(parser.nextText());
                    } else parser.next();
                    break;
                case XmlPullParser.END_TAG:
                    if (IncomingNewXTokenIQ.ELEMENT.equals(parser.getName())) {
                        break outerloop;
                    } else parser.next();
                    break;
                default:
                    parser.next();
            }
        }

        if (token != null && tokenUID != null)
            return new IncomingNewXTokenIQ(token, tokenUID, TimeUnit.SECONDS.toMillis(expire));
        else return null;
    }
}
