package com.xabber.android.data.extension.xtoken;

import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import java.util.concurrent.TimeUnit;

public class XTokenProvider extends IQProvider<XTokenIQ> {

    @Override
    public XTokenIQ parse(XmlPullParser parser, int initialDepth) throws Exception {

        String token = null;
        String tokenUID = null;
        long expire = 0;

        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (XTokenIQ.ELEMENT.equals(parser.getName())
                            && XTokenIQ.NAMESPACE.equals(parser.getNamespace())) {
                        parser.next();
                    } else if (XTokenIQ.ELEMENT_TOKEN.equals(parser.getName())) {
                        token = parser.nextText();
                    } else if (XTokenIQ.ELEMENT_TOKEN_UID.equals(parser.getName())) {
                        tokenUID = parser.nextText();
                    } else if (XTokenIQ.ELEMENT_EXPIRE.equals(parser.getName())) {
                        expire = Long.valueOf(parser.nextText());
                    } else parser.next();
                    break;
                case XmlPullParser.END_TAG:
                    if (XTokenIQ.ELEMENT.equals(parser.getName())) {
                        break outerloop;
                    } else parser.next();
                    break;
                default:
                    parser.next();
            }
        }

        if (token != null && tokenUID != null)
            return new XTokenIQ(token, tokenUID, TimeUnit.SECONDS.toMillis(expire));
        else return null;
    }
}
