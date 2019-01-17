package com.xabber.android.data.xaccount;

import android.util.Log;

import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

public class HttpConfirmIqProvider extends IQProvider<HttpConfirmIq> {

    @Override
    public HttpConfirmIq parse(XmlPullParser parser, int initialDepth) throws Exception {
        HttpConfirmIq httpConfirmIq = new HttpConfirmIq();

        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (parser.getEventType() == XmlPullParser.START_TAG
                            && HttpConfirmIq.ELEMENT.equals(parser.getName())
                            && HttpConfirmIq.NAMESPACE.equals(parser.getNamespace())) {
                        try {
                            httpConfirmIq.setUrl(parser.getAttributeValue("", HttpConfirmIq.PROP_URL));
                            httpConfirmIq.setId(parser.getAttributeValue("", HttpConfirmIq.PROP_ID));
                            httpConfirmIq.setMethod(parser.getAttributeValue("", HttpConfirmIq.PROP_METHOD));
                            break outerloop;
                        } catch (Exception e) {
                            Log.d("HttpConfirmIqProvider", "error in parsing: " + e.toString());
                            break outerloop;
                        }
                    } else parser.next();
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                    break;
                default:
                    parser.next();
            }
        }
        return httpConfirmIq;
    }
}
